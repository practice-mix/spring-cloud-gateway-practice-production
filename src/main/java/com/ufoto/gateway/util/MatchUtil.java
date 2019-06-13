package com.ufoto.gateway.util;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;

import java.util.List;

/**
 * @author Luo Bao Ding
 * @since 2018/5/31
 */
public class MatchUtil {

    private static PathMatcher pathMatcher = new AntPathMatcher();

    public static boolean matches(String lookupPath, List<String> excludePatterns, List<String> includePatterns) {
        if (excludePatterns != null) {
            for (String pattern : excludePatterns) {
                if (pathMatcher.match(pattern, lookupPath)) {
                    return false;
                }
            }
        }
        if (ObjectUtils.isEmpty(includePatterns)) {
            return true;
        } else {
            for (String pattern : includePatterns) {
                if (pathMatcher.match(pattern, lookupPath)) {
                    return true;
                }
            }
            return false;
        }
    }

}
