package lyzzcw.work.component.util;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lyzzcw.work.component.constant.Header;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpHeaders;

import java.util.Objects;

/**
 * @author lzy
 * @version 1.0
 * Date: 2023/6/26 19:13
 * Description: No Description
 */
public class EnvUtils {

    public static final String HOST_NAME_VALUE = "${HOSTNAME}";

    public static String getTag(HttpHeaders headers) {
        String tag = headers.getFirst(Header.TAG.getValue());
        // 如果请求的是 "${HOSTNAME}"，则解析成对应的本地主机名
        // 目的：特殊逻辑，解决 IDEA Rest Client 不支持环境变量的读取，所以就服务器来做
        return Objects.equals(tag, HOST_NAME_VALUE) ? getHostName() : tag;
    }

    public static String getTag(ServiceInstance instance) {
        return instance.getMetadata().get(Header.TAG.getValue());
    }

    public static String getHostName() {
        return StrUtil.blankToDefault(NetUtil.getLocalHostName(), IdUtil.fastSimpleUUID());
    }

}
