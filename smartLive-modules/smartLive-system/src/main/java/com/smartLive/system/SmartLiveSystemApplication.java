package com.smartLive.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.smartLive.common.security.annotation.EnableCustomConfig;
import com.smartLive.common.security.annotation.EnableRyFeignClients;

/**
 * 系统模块
 * 
 * @author smartLive
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class SmartLiveSystemApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(SmartLiveSystemApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  系统模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " __  __  _   _  __  __  _   _  _      ___  _   _ \n" +
                "|  \\/  || | | ||  \\/  || | | || |    |_ _|| \\ | |\n" +
                "| |\\/| || | | || |\\/| || | | || |     | | |  \\| |\n" +
                "| |  | || |_| || |  | || |_| || |___  | | | |\\  |\n" +
                "|_|  |_| \\___/ |_|  |_| \\___/ |_____||___||_| \\_|\n");
    }
}
