package com.tt.consumer.client;

import com.tt.consumer.service.UseClientImpl;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 有关消费者client,配置需要调用服务者的名字
 */
@FeignClient(name = "tt-sc-feign-provide",fallback = UseClientImpl.class) //对应的要调用提供者服务名spring.application.name
public interface UserClient {

    /**
     * 通过用户id获取用户
     * @param userId
     * @return
     */
    @GetMapping("/user/getUser/{userId}")//对应要调用提供者的controller
    String getUser(@PathVariable("userId") String userId);
}
