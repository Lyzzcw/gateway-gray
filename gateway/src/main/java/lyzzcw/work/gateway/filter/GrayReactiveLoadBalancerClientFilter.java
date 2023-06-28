package lyzzcw.work.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import lyzzcw.work.component.constant.Header;
import lyzzcw.work.gateway.loadbalance.GrayLoadBalancer;
import lyzzcw.work.gateway.loadbalance.GrayEnvLoadBalancer;
import lyzzcw.work.gateway.properties.ServerGrayProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.Set;


/**
 * @author lzy
 * @version 1.0
 * Date: 2023/6/26 18:55
 * Description:
 * 支持灰度功能的 {@link ReactiveLoadBalancerClientFilter} 实现类
 * <p>
 * 由于 {@link ReactiveLoadBalancerClientFilter} 中的都是 private 方法，无法进行重写。
 * 因此，这里只好 copy 它所有的代码，手动重写 choose 方法
 * <p>
 * 具体的使用与实现原理，可阅读如下两个文章：
 * 1. https://www.jianshu.com/p/6db15bc0be8f
 * 2. https://cloud.tencent.com/developer/article/1620795
 */

@Component
@Slf4j
public class GrayReactiveLoadBalancerClientFilter implements GlobalFilter, Ordered {

    private final LoadBalancerClientFactory clientFactory;

    private final GatewayLoadBalancerProperties properties;

    private final LoadBalancerProperties loadBalancerProperties;

    public GrayReactiveLoadBalancerClientFilter(LoadBalancerClientFactory clientFactory,
                                                GatewayLoadBalancerProperties properties,
                                                LoadBalancerProperties loadBalancerProperties) {
        this.clientFactory = clientFactory;
        this.properties = properties;
        this.loadBalancerProperties = loadBalancerProperties;
    }

    @Autowired
    private ServerGrayProperty serverGrayProperty;

    @Override
    public int getOrder() {
        return ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = (URI) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = (String) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR);
        // 将 lb 替换成 grayLb，表示灰度负载均衡
        if (url != null && ("grayLb".equals(url.getScheme()) || "grayLb".equals(schemePrefix))) {
            ServerWebExchangeUtils.addOriginalRequestUrl(exchange, url);
            if (log.isTraceEnabled()) {
                log.trace(ReactiveLoadBalancerClientFilter.class.getSimpleName() + " url before: " + url);
            }

            URI requestUri = (URI) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
            String serviceId = requestUri.getHost();
            Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator.getSupportedLifecycleProcessors(this.clientFactory.getInstances(serviceId, LoadBalancerLifecycle.class), RequestDataContext.class, ResponseData.class, ServiceInstance.class);
            DefaultRequest<RequestDataContext> lbRequest = new DefaultRequest(new RequestDataContext(new RequestData(exchange.getRequest()), this.getHint(serviceId, this.loadBalancerProperties.getHint())));
            return this.choose(lbRequest, serviceId, supportedLifecycleProcessors).doOnNext((response) -> {
                if (!response.hasServer()) {
                    supportedLifecycleProcessors.forEach((lifecycle) -> {
                        lifecycle.onComplete(new CompletionContext(CompletionContext.Status.DISCARD, lbRequest, response));
                    });
                    throw NotFoundException.create(this.properties.isUse404(), "Unable to find instance for " + url.getHost());
                } else {
                    ServiceInstance retrievedInstance = (ServiceInstance) response.getServer();
                    URI uri = exchange.getRequest().getURI();
                    String overrideScheme = retrievedInstance.isSecure() ? "https" : "http";
                    if (schemePrefix != null) {
                        overrideScheme = url.getScheme();
                    }

                    DelegatingServiceInstance serviceInstance = new DelegatingServiceInstance(retrievedInstance, overrideScheme);
                    URI requestUrl = this.reconstructURI(serviceInstance, uri);
                    if (log.isTraceEnabled()) {
                        log.trace("LoadBalancerClientFilter url chosen: " + requestUrl);
                    }

                    exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, requestUrl);
                    exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR, response);
                    supportedLifecycleProcessors.forEach((lifecycle) -> {
                        lifecycle.onStartRequest(lbRequest, response);
                    });
                }
            }).then(chain.filter(exchange)).doOnError((throwable) -> {
                supportedLifecycleProcessors.forEach((lifecycle) -> {
                    lifecycle.onComplete(new CompletionContext(CompletionContext.Status.FAILED, throwable, lbRequest, (Response) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR)));
                });
            }).doOnSuccess((aVoid) -> {
                supportedLifecycleProcessors.forEach((lifecycle) -> {
                    lifecycle.onComplete(new CompletionContext(CompletionContext.Status.SUCCESS, lbRequest, (Response) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR), new ResponseData(exchange.getResponse(), new RequestData(exchange.getRequest()))));
                });
            });
        } else {
            return chain.filter(exchange);
        }
    }

    protected URI reconstructURI(ServiceInstance serviceInstance, URI original) {
        return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
    }

    private Mono<Response<ServiceInstance>> choose(Request<RequestDataContext> lbRequest, String serviceId,
                                                   Set<LoadBalancerLifecycle> supportedLifecycleProcessors) {
        if (serverGrayProperty.getEnable()) {
            // 直接创建 GrayEnvLoadBalancer 对象
            GrayEnvLoadBalancer loadBalancer = new GrayEnvLoadBalancer(
                    clientFactory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class), serviceId);
            loadBalancer.setServerGrayProperty(serverGrayProperty);
            supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
            return loadBalancer.choose(lbRequest);
        } else {
            // 直接创建 GrayLoadBalancer 对象
            GrayLoadBalancer loadBalancer = new GrayLoadBalancer(
                    clientFactory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class), serviceId);
            loadBalancer.setServerGrayProperty(serverGrayProperty);
            supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
            return loadBalancer.choose(lbRequest);
        }
    }

    private String getHint(String serviceId, Map<String, String> hints) {
        String defaultHint = (String) hints.getOrDefault("default", "default");
        String hintPropertyValue = (String) hints.get(serviceId);
        return hintPropertyValue != null ? hintPropertyValue : defaultHint;
    }

}
