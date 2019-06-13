package com.ufoto.gateway.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Luo Bao Ding
 * @since 2018/6/2
 */
public class EncryptKeyUtilTest {

    @Test
    public void getMD5() {
        String md5 = EncryptKeyUtil.getMD5("/ft/auth/include/get?token=1");
        Assert.assertNotNull(md5);
        System.out.println("md5 = " + md5);
    }
}
