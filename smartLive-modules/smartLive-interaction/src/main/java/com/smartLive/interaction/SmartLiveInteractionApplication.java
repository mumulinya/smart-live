package com.smartLive.interaction;

import com.smartLive.common.security.annotation.EnableCustomConfig;
import com.smartLive.common.security.annotation.EnableRyFeignClients;
import com.smartLive.interaction.strategy.like.LikeStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统模块
 * 
 * @author smartLive
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
//开启定时任务
@EnableScheduling
public class SmartLiveInteractionApplication
{
    public static void main(String[] args)
    {
        ConfigurableApplicationContext context = SpringApplication.run(SmartLiveInteractionApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  互动模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
        Map<Integer, LikeStrategy> likeStrategyMap = context.getBean("LikeStrategyMap", Map.class);
        likeStrategyMap.forEach((k, v) -> System.out.println("key:"+k+",value:"+v.getType()));
    }
}
