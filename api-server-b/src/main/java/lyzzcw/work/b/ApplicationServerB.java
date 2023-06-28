package lyzzcw.work.b;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author lzy
 * @version 1.0
 * Date: 2023/6/27 9:09
 * Description: No Description
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "lyzzcw.work.component.client") //注入feign
public class ApplicationServerB {
    public static void main(String[] args) {
        SpringApplication.run(ApplicationServerB.class, args);
    }
}
