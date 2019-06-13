package com.ufoto.gateway.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luo Bao Ding
 * @since 2018/5/29
 */
@ConfigurationProperties(prefix = "login-auth")
@Component
public class LoginAuthConfig implements BaseAuthConfig {
    private List<String> excludePatterns = new ArrayList<>();

    private List<String> includePatterns = new ArrayList<>();

    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    public List<String> getIncludePatterns() {
        return includePatterns;
    }

    public void setIncludePatterns(List<String> includePatterns) {
        this.includePatterns = includePatterns;
    }
}
