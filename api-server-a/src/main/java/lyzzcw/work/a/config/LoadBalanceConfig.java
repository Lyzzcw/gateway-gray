package lyzzcw.work.a.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Created with IntelliJ IDEA.
 * @author: lzy
 * Date: 2021/12/15
 * Time: 13:48
 * Description: 传统开启负载均衡方式
 */
//@Configuration
public class LoadBalanceConfig {
    /**
     * {@link org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration}
     * {@link org.springframework.cloud.loadbalancer.config.BlockingLoadBalancerClientAutoConfiguration}
     * @return
     */
//    @LoadBalanced
//    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
