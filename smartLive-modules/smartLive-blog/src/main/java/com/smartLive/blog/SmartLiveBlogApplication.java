package com.smartLive.blog;

import com.smartLive.common.security.annotation.EnableCustomConfig;
import com.smartLive.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 博客模块启动类。
 *
 * @author smartLive
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class SmartLiveBlogApplication
{
    /**
     * 启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args)
    {
        SpringApplication.run(SmartLiveBlogApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  博客模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " __  __  _   _  __  __  _   _  _      ___  _   _ \n" +
                "|  \\/  || | | ||  \\/  || | | || |    |_ _|| \\ | |\n" +
                "| |\\/| || | | || |\\/| || | | || |     | | |  \\| |\n" +
                "| |  | || |_| || |  | || |_| || |___  | | | |\\  |\n" +
                "|_|  |_| \\___/ |_|  |_| \\___/ |_____||___||_| \\_|\n");
    }
}
