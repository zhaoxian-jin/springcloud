package com.tt.feignprovide;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
/**
 * 服务提供方-供feignclient使用
 **/
@EnableDiscoveryClient
@SpringBootApplication
public class NacosFeignProvideApplication {
    public static void main(String[] args) {
        SpringApplication.run(NacosFeignProvideApplication.class, args);
    }

}
