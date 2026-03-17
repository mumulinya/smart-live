package com.smartLive.ai;

import com.smartLive.common.security.annotation.EnableCustomConfig;
import com.smartLive.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
/**
 * AI 模块启动类。
 */
@EnableCustomConfig
@EnableRyFeignClients
public class SmartLiveAiApplication {

    /**
     * 启动 AI 模块应用。
     */
    public static void main(String[] args) {
        SpringApplication.run(SmartLiveAiApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  AI服务模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " __  __  _   _  __  __  _   _  _      ___  _   _ \n" +
                "|  \\/  || | | ||  \\/  || | | || |    |_ _|| \\ | |\n" +
                "| |\\/| || | | || |\\/| || | | || |     | | |  \\| |\n" +
                "| |  | || |_| || |  | || |_| || |___  | | | |\\  |\n" +
                "|_|  |_| \\___/ |_|  |_| \\___/ |_____||___||_| \\_|\n");
    }
}
