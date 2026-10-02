package org.example.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.util.List;

/** Technical scope and measurement conventions: see docs/IMPLEMENTATION_SCOPE.md. */
@Component
public class ServiceDiscoveryUtil {

    @Autowired
    private DiscoveryClient discoveryClient;

    public List<ServiceInstance> getServiceInstancesByPrefix(String serviceTypePrefix) {
        List<String> allServices = discoveryClient.getServices();
        return allServices.stream()
                .filter(serviceName -> serviceName.startsWith(serviceTypePrefix))
                .flatMap(serviceName -> discoveryClient.getInstances(serviceName).stream())
                .toList();
    }
}
