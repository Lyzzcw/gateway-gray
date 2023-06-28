package lyzzcw.work.a.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author lzy
 * @version 1.0
 * Date: 2023/6/27 15:51
 * Description: No Description
 */
public class FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        Map<String, String> headers = getHeaders();
        headers.forEach(requestTemplate::header);
    }

    /**
     * 获取 request 中的所有的 header 值
     *
     * @return
     */
    private Map<String, String> getHeaders() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Map<String, String> map = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            if(HttpHeaders.CONTENT_LENGTH.equals(key)){
                continue;
            }
            String value = request.getHeader(key);
            map.put(key, value);
        }
        return map;
    }


}
