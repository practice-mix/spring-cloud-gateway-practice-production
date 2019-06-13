package com.ufoto.gateway.common;

import com.ufoto.gateway.contants.GatewayConstants;
import com.ufoto.gateway.util.LogUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 访问日志过滤器工厂
 *
 * @author Luo Bao Ding
 * @since 2018/5/18
 */
@Component
public class AccessLogGlobalFilter implements GlobalFilter, Ordered {
    public static final Logger logger = LogManager.getLogger("accessStatLog");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put(GatewayConstants.FLT_ATT_KEY_ELAPSED_TIME_BEGIN, System.currentTimeMillis());
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            String msg = null;
            try {
                msg = LogUtil.buildLogMsg(exchange);
            } catch (Exception e) {
                e.printStackTrace();
                msg = "build log error";
            }
            logger.info(msg);

        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
