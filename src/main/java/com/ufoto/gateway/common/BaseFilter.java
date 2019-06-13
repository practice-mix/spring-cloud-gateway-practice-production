package com.ufoto.gateway.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author Luo Bao Ding
 * @since 2018/5/31
 */
public abstract class BaseFilter extends AbstractGatewayFilterFactory {
    protected FilterSupport filterSupport;

    protected StringRedisTemplate redisTpl;

    @Autowired
    public void setRedisTpl(StringRedisTemplate redisTpl) {
        this.redisTpl = redisTpl;
    }

    @Autowired
    public void setFilterSupport(FilterSupport filterSupport) {
        this.filterSupport = filterSupport;
    }

}
