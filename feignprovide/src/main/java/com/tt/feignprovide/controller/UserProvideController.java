package com.tt.feignprovide.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "用户模块")
@RestController
@RequestMapping("user")
public class UserProvideController {

    @Value("${server.port}")
    private String port;

    /**
     * 通过用户ID获取用户
     * @param userId
     * @return
     */
    @ApiOperation(value = "根据用户ID查询用户")
    @GetMapping("/getUser/{userId}")
    public String getUser(@PathVariable("userId") String userId){

        return String.format("【%s-服务提供者】：%s", port, userId);
    }


}
