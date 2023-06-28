package lyzzcw.work.component.util;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.naming.core.Balancer;
import org.springframework.cloud.client.ServiceInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lzy
 * @version 1.0
 * Date: 2023/6/26 19:22
 * Description: No Description
 */
public class CustomizeBalancer extends Balancer {
    public CustomizeBalancer() {
    }

    public static Instance getHostByRandomWeight2(List<Instance> instances) {
        return Balancer.getHostByRandomWeight(instances);
    }

    public static ServiceInstance getHostByRandomWeight3(List<ServiceInstance> serviceInstances) {
        Map<Instance, ServiceInstance> instanceMap = new HashMap();
        List<Instance> nacosInstance = (List)serviceInstances.stream().map((serviceInstance) -> {
            Map<String, String> metadata = serviceInstance.getMetadata();
            Instance instance = new Instance();
            instance.setIp(serviceInstance.getHost());
            instance.setPort(serviceInstance.getPort());
            instance.setWeight(Double.parseDouble((String)metadata.get("nacos.weight")));
            instance.setHealthy(Boolean.parseBoolean((String)metadata.get("nacos.healthy")));
            instanceMap.put(instance, serviceInstance);
            return instance;
        }).collect(Collectors.toList());
        Instance instance = getHostByRandomWeight2(nacosInstance);
        return (ServiceInstance)instanceMap.get(instance);
    }
}
