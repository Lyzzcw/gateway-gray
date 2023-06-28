package lyzzcw.work.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lzy
 * @version 1.0
 * Date: 2023/6/27 0:06
 * Description: No Description
 */
@RestController
@RequestMapping("api")
public class GatewayController {
    @GetMapping("test")
    public String test(){
        return "hello world";
    }
}
