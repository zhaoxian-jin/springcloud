package com.tt.consumer.service;

import com.tt.consumer.client.UserClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

@Component
public class UseClientImpl implements UserClient {
    //熔断方法
    @Override
    public String getUser(@PathVariable("userId") String userId) {
        return "服务出错，返回信息";
    }
}
