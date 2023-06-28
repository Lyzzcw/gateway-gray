package lyzzcw.work.a;

import lyzzcw.work.a.config.GrayLoadBalancerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author lzy
 * @version 1.0
 * Date: 2023/6/27 9:09
 * Description: No Description
 */
@ComponentScan(basePackages = {"lyzzcw.work.component","lyzzcw.work.a"})
@SpringBootApplication
@EnableFeignClients(basePackages = "lyzzcw.work.component.client") //注入feign
@LoadBalancerClients(
        value = {@LoadBalancerClient(value = "api-server-b",configuration = GrayLoadBalancerConfiguration.class)},
        defaultConfiguration = LoadBalancerAutoConfiguration.class)
//@LoadBalancerClients(defaultConfiguration = GrayLoadBalancerConfiguration.class)
public class ApplicationServerA {
    public static void main(String[] args) {
        SpringApplication.run(ApplicationServerA.class, args);
    }
}
