package com.smartLive.order;

import com.smartLive.common.security.annotation.EnableCustomConfig;
import com.smartLive.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 订单模块启动类。
 *
 * @author smartLive
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class SmartLiveOrderApplication
{
    /**
     * 启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args)
    {
        SpringApplication.run(SmartLiveOrderApplication.class, args);
        System.out.println("订单模块启动成功");
    }
}
