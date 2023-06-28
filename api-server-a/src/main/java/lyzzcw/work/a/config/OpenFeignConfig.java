package lyzzcw.work.a.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * @author: lzy
 * Date: 2021/12/16
 * Time: 16:41
 * Description: 调整日志等级
 */
@Configuration
public class OpenFeignConfig {

//    @Bean
//    @ConditionalOnMissingBean
//    public HttpMessageConverters messageConverters(ObjectProvider<HttpMessageConverter<?>> converters) {
//        return new HttpMessageConverters(converters.orderedStream().collect(Collectors.toList()));
//    }

    @Bean
    Logger.Level feignLoggerLevel(){
        return  Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor getRequestInterceptor() {
        return new FeignRequestInterceptor();
    }

}
