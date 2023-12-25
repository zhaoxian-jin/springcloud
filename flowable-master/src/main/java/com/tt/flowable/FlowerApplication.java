package com.tt.flowable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication/*(exclude = {DataSourceAutoConfiguration.class})*/
public class FlowerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlowerApplication.class, args);
    }
}
