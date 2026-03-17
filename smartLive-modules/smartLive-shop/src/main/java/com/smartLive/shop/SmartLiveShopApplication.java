package com.smartLive.shop;

import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.security.annotation.EnableCustomConfig;
import com.smartLive.common.security.annotation.EnableRyFeignClients;
import com.smartLive.shop.domain.Shop;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商铺模块启动类。
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class SmartLiveShopApplication
{
    /**
     * 启动商铺模块。
     */
    public static void main(String[] args)
    {
        SpringApplication.run(SmartLiveShopApplication.class, args);
        System.out.println("===== SHOP JAR VERSION 2025-01-13 20:30 =====");
        System.out.println("(♥◠‿◠)ﾉﾞ  店铺模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " __  __  _   _  __  __  _   _  _      ___  _   _ \n" +
                "|  \\/  || | | ||  \\/  || | | || |    |_ _|| \\ | |\n" +
                "| |\\/| || | | || |\\/| || | | || |     | | |  \\| |\n" +
                "| |  | || |_| || |  | || |_| || |___  | | | |\\  |\n" +
                "|_|  |_| \\___/ |_|  |_| \\___/ |_____||___||_| \\_|\n");
    }

}
