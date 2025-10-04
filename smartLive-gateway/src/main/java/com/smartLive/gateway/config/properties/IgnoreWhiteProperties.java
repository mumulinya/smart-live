package com.smartLive.gateway.config.properties;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * 放行白名单配置
 * 
 * @author smartLive
 */
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "security.ignore")
public class IgnoreWhiteProperties
{
    /**
     * 放行管理端白名单配置，网关不校验此处的白名单
     */
    private List<String> adminWhites = new ArrayList<>();

    /**
     * 放行APP白名单配置，网关不校验此处的白名单
     */
    private List<String> appWhites = new ArrayList<>();

    public List<String> getAdminWhites() {
        return adminWhites;
    }

    public void setAdminWhites(List<String> adminWhites) {
        this.adminWhites = adminWhites;
    }

    public List<String> getAppWhites() {
        return appWhites;
    }

    public void setAppWhites(List<String> appWhites) {
        this.appWhites = appWhites;
    }
}
