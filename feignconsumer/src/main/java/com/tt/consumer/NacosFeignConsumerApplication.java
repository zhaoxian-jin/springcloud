package com.tt.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
//import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * 代表自己是一个服务消费者
 */
@SpringBootApplication
/**
 * 声明此服务可作为消费者使用，配置feign客户端目录
 */
@EnableFeignClients(basePackages = "com.tt.consumer.client")
//开启feign的负载均衡
//@EnableEurekaClient
public class NacosFeignConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(NacosFeignConsumerApplication.class, args);
    }
}
