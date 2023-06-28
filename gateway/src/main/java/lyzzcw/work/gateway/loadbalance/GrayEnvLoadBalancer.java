package lyzzcw.work.gateway.loadbalance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lyzzcw.work.component.constant.Header;
import lyzzcw.work.gateway.properties.ServerGrayProperty;
import lyzzcw.work.component.util.CustomizeBalancer;
import lyzzcw.work.component.util.EnvUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author lzy
 * @version 1.0
 * Date: 2023/6/26 18:55
 * Description: No Description
 */
@RequiredArgsConstructor
@Slf4j
public class GrayEnvLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private static final String GRAY_MARK = "userId";

    private ServerGrayProperty serverGrayProperty;
    /**
     * 用于获取 serviceId 对应的服务实例的列表
     */
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    /**
     * 需要获取的服务实例名
     * <p>
     * 暂时用于打印 logger 日志
     */
    private final String serviceId;

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        // 获得 HttpHeaders 属性，实现从 header 中获取 version
        HttpHeaders headers = HttpHeaders.writableHttpHeaders(
                ((RequestDataContext) request.getContext()).getClientRequest().getHeaders());
        // 选择实例
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get(request).next().map(list -> getInstanceResponse(list, headers));
    }

    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances, HttpHeaders headers) {
        // 如果服务实例为空，则直接返回
        if (CollUtil.isEmpty(instances)) {
            log.warn("[getInstanceResponse][serviceId({}) 服务实例列表为空]", serviceId);
            return new EmptyResponse();
        }
        // todo 这里获取可以根据登录的信息获取
        String userId = Optional.ofNullable(headers.getFirst(GRAY_MARK)).orElse(StrUtil.EMPTY);
        // 筛选满足 version 条件的实例列表
        String grayVersion = serverGrayProperty.getGrayVersion();
        String proVersion = serverGrayProperty.getProVersion();
        // 筛选满足 灰度的用户实例列表
        List<String> grayUsers = serverGrayProperty.getGrayUsers();
        List<ServiceInstance> chooseInstances;
        if (StrUtil.isNotEmpty(proVersion) && !grayUsers.contains(userId)) {
            chooseInstances = filterList(instances, instance -> proVersion.equals(instance.getMetadata().get(Header.VERSION.getValue())));
            headers.set(Header.VERSION.getValue(), proVersion);
        } else {
            // 选择满足条件的实例
            chooseInstances = filterList(instances, instance -> grayVersion.equals(instance.getMetadata().get(Header.VERSION.getValue())) && grayUsers.contains(userId));
            headers.set(Header.VERSION.getValue(), grayVersion);
            if (CollUtil.isEmpty(chooseInstances)) {
                log.warn("[getInstanceResponse][serviceId({}) 没有满足版本({})的服务实例列表，直接使用所有服务实例列表]", serviceId, grayVersion);
                chooseInstances = instances;
                headers.set(Header.VERSION.getValue(), proVersion);
            }
        }

        // 基于 tag 过滤实例列表
        chooseInstances = filterTagServiceInstances(chooseInstances, headers);

        // 随机 + 权重获取实例列表
        return new DefaultResponse(
                CustomizeBalancer.getHostByRandomWeight3(chooseInstances));
    }


    /**
     * 基于 tag 请求头，过滤匹配 tag 的服务实例列表
     * <p>
     * copy from EnvLoadBalancerClient
     *
     * @param instances 服务实例列表
     * @param headers   请求头
     * @return 服务实例列表
     */
    private List<ServiceInstance> filterTagServiceInstances(List<ServiceInstance> instances, HttpHeaders headers) {
        // 情况一，没有 tag 时，直接返回
        String tag = EnvUtils.getTag(headers);
        if (StrUtil.isEmpty(tag)) {
            return instances;
        }

        // 情况二，有 tag 时，使用 tag 匹配服务实例
        List<ServiceInstance> chooseInstances = filterList(instances, instance -> tag.equals(EnvUtils.getTag(instance)));
        if (CollUtil.isEmpty(chooseInstances)) {
            log.warn("[filterTagServiceInstances][serviceId({}) 没有满足 tag({}) 的服务实例列表，直接使用所有服务实例列表]", serviceId, tag);
            chooseInstances = instances;
        }
        return chooseInstances;
    }

    public static <T> List<T> filterList(Collection<T> from, Predicate<T> predicate) {
        if (CollUtil.isEmpty(from)) {
            return new ArrayList<>();
        }
        return from.stream().filter(predicate).collect(Collectors.toList());
    }


    public void setServerGrayProperty(ServerGrayProperty serverGrayProperty) {
        this.serverGrayProperty = serverGrayProperty;
    }
}
