package lyzzcw.work.component.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author lzy
 * @version 1.0
 * Date: 2023/6/27 8:43
 * Description: No Description
 */
@FeignClient(value = "api-server-b",path = "api/b/")
public interface ApiServerBFeign {

    @GetMapping("get/{id}")
    String get(@PathVariable("id")String id);

}
