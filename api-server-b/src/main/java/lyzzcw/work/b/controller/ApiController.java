package lyzzcw.work.b.controller;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lzy
 * @version 1.0
 * Date: 2023/6/26 23:29
 * Description: No Description
 */
@RestController
@RequestMapping("api/b")
@RequiredArgsConstructor
public class ApiController {

    final NacosDiscoveryProperties nacosDiscoveryProperties;

    @GetMapping("get/{id}")
    public String get(@PathVariable("id")String id){
        return ">>>>>>>>> b -> version:" + nacosDiscoveryProperties.getMetadata().get("version")
                + ",tag:" + nacosDiscoveryProperties.getMetadata().get("tag")
                + ",id:" + id;
    }

}
