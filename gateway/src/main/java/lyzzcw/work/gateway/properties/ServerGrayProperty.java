package lyzzcw.work.gateway.properties;

import lombok.Data;
import lyzzcw.work.gateway.loadbalance.GrayEnvLoadBalancer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author lzy
 * @version 1.0
 * Date: 2023/6/26 18:55
 * Description: No Description
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "server.gray")
public class ServerGrayProperty {

    /**
     * 生产的版本
     */
    private String proVersion;
    /**
     * 需要灰度的策略 - 这里简单用id来测试
     */
    private List<String> grayUsers;
    /**
     * 灰度的版本
     */
    private String grayVersion;
    /**
     * 是否开启{@link GrayEnvLoadBalancer} 的方式进行灰度发布
     */
    private Boolean enable = true;
}
