package com.tt.consumer.controller;

import com.tt.consumer.client.UserClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 平台controller业务逻辑，引入client出发消费者调用提供者
 */
@Api(tags = "用户模块")
@RestController
@RequestMapping("user")
public class FeignConsumerController {

    @Value("${server.port}")
    private String port;

    @Resource
    private UserClient userClient;

    /**
     * 通过用户ID获取用户
     * @param userId
     * @return
     */
    @ApiOperation(value = "根据用户ID查询用户")
    @GetMapping("/getUserInfo/{userId}")
    public String getUserInfo(@PathVariable("userId") String userId){

        String user = userClient.getUser(userId);
        return String.format("【%s-Demo消费者】：调用Feign接口返回值 %s", port, user);
    }
    /**
     * 测试sentinel的url流控
     * @return
     */
    @GetMapping("/getSentinel")
    public String getUserInfo() throws InterruptedException {
        Thread.sleep(3000);
        return String.format("【%s-Demo消费者】：调用Feign接口返回值getSentinel", "sentinel");
    }

}
