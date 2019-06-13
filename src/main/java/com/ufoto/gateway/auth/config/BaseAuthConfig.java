package com.ufoto.gateway.auth.config;

import com.ufoto.gateway.util.MatchUtil;

import java.util.List;

/**
 * @author Luo Bao Ding
 * @since 2018/5/31
 */
public interface BaseAuthConfig {

    List<String> getExcludePatterns();

    List<String> getIncludePatterns();

    default boolean isRequireAuth(String path) {
        List<String> includePatterns = getIncludePatterns();
        List<String> excludePatterns = getExcludePatterns();

        return MatchUtil.matches(path, excludePatterns, includePatterns);
    }
}
