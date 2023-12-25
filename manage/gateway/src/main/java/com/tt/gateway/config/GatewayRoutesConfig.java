package com.tt.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gateway配置类
 */
//@Configuration
public class GatewayRoutesConfig {
    /**
     * 通过代码配置路由
     * @param builder
     * @return
     */
    //@Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder){
        return builder.routes()
                .route("gwcloud-user-provide-service-code", r -> r.path("/user/**")
                        .uri("lb://gwcloud-user-provide-service"))
                .build();
    }

}
