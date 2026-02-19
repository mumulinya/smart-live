package com.smartLive.product;

import com.smartLive.common.security.annotation.EnableCustomConfig;
import com.smartLive.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 商品模块
 * 
 * @author smartLive
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class SmartLiveProductApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(SmartLiveProductApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  商品模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                "  ____                 _            _   \n" +
                " |  _ \\  _ __   ___   | | _   _    ___ | |_ \n" +
                " | |_) || '__| / _ \\  | || | | |  / __|| __|\n" +
                " |  __/ | |   | (_) | | || |_| | | (__ | |_ \n" +
                " |_|    |_|    \\___/  |_| \\__,_|  \\___| \\__|\n");
    }
}
