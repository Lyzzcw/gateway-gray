package lyzzcw.work.a.controller;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lyzzcw.work.component.client.ApiServerBFeign;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.CompletableFuture;

/**
 * @author lzy
 * @version 1.0
 * Date: 2023/6/26 23:29
 * Description: No Description
 */
@RestController
@RequestMapping("/system/api/")
@RequiredArgsConstructor
public class ApiController {

    final ApiServerBFeign apiServerBFeign;

    final NacosDiscoveryProperties nacosDiscoveryProperties;

    @SneakyThrows
    @GetMapping("get/{id}")
    public String get(@PathVariable("id")String id){

        /**
         * 默认的 {@link org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient}
         *                  ↓
         * Response<ServiceInstance> loadBalancerResponse = (Response)Mono.from(loadBalancer.choose(request)).block();
         */

        //
        RequestContextHolder.setRequestAttributes(RequestContextHolder.getRequestAttributes(),true);
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            return apiServerBFeign.get(id);
        });
        return "a -> version:" + nacosDiscoveryProperties.getMetadata().get("version")
                + ",tag:" + nacosDiscoveryProperties.getMetadata().get("tag")
                + "," + completableFuture.get();
    }


    @SneakyThrows
    @GetMapping("pull/{id}")
    public String pull(@PathVariable("id")String id){

        return "a -> version:" + nacosDiscoveryProperties.getMetadata().get("version")
                + ",tag:" + nacosDiscoveryProperties.getMetadata().get("tag")
                + "," + apiServerBFeign.get(id);
    }
}
