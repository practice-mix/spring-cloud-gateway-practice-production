package com.ufoto.gateway.breaker.appapi;

import com.ufoto.gateway.enums.GatewayErrorCodeEnum;
import com.ufoto.gateway.result.compatible.ApiResult;
import com.ufoto.gateway.util.LogUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;

/**
 * ufoto-app-api的熔断响应
 * <p>
 * Created by LuoBaoding on 2018/5/7
 */
@RestController
public class AppApiController {
    public static final Logger logger = LogManager.getLogger("breakerLog");

    @RequestMapping("/breaker/app-api/fallback")
    public ApiResult fallback(ServerWebExchange exchange) {
        String message = null;
        try {
            ServerWebExchange delegate = ((ServerWebExchangeDecorator) exchange).getDelegate();
            message = LogUtil.buildLogMsg(delegate);
        } catch (Exception e) {
            e.printStackTrace();
            message = "build log error";
        }
        logger.warn(message);
        GatewayErrorCodeEnum breakerErrEnum = GatewayErrorCodeEnum.APP_API_CIRCUIT_BREAKER;
        return new ApiResult(Integer.valueOf(breakerErrEnum.getErrorCode()), breakerErrEnum.getDescription());
    }
}
