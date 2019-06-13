package com.ufoto.gateway.util;

import com.ufoto.gateway.auth.config.LoginAuthConfig;
import com.ufoto.gateway.starter.GatewayApplication;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * @author Luo Bao Ding
 * @since 2018/6/2
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = GatewayApplication.class)
public class MatchUtilTest {

    @Autowired
    private LoginAuthConfig loginAuthConfig;

    @Test
    public void testMatches() {
        List<String> excludePatterns = loginAuthConfig.getExcludePatterns();
        List<String> includePatterns = loginAuthConfig.getIncludePatterns();
        Assert.assertFalse(MatchUtil.matches("/challgengeVote.html", excludePatterns, includePatterns));
        Assert.assertFalse(MatchUtil.matches("/testApi", excludePatterns, includePatterns));
        Assert.assertFalse(MatchUtil.matches("/testApi/", excludePatterns, includePatterns));
        Assert.assertFalse(MatchUtil.matches("/testApi/xx", excludePatterns, includePatterns));
        Assert.assertFalse(MatchUtil.matches("/error", excludePatterns, includePatterns));
        Assert.assertFalse(MatchUtil.matches("/activityApi/randPhotos", excludePatterns, includePatterns));
        Assert.assertFalse(MatchUtil.matches("/activityApi/randPhotos/", excludePatterns, includePatterns));
        Assert.assertFalse(MatchUtil.matches("/act/fl/xx/xx/xx/xx", excludePatterns, includePatterns));
        Assert.assertTrue(MatchUtil.matches("/xx", excludePatterns, includePatterns));

    }
}