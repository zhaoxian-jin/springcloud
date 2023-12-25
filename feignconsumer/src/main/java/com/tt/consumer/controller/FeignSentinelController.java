package com.tt.consumer.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("user")
public class FeignSentinelController {

    /**
     * 测试sentinel的流控
     * @return
     */
    @GetMapping("/getUserSentinel")
    //@SentinelResource(value = "userSentinel",blockHandler = "resultSentinel")
    public String getUserInfo(){
        return String.format("【%s-Demo消费者】：调用Feign接口返回值getUserSentinel", "sentinel");
    }


    public String resultSentinel(BlockException exception){
        return "feignconsumer限流成功！";
    }
  /*  public String handleException(BlockException exception) {
        return exception.getClass().getCanonicalName()+"限流了";
    }*/

}
