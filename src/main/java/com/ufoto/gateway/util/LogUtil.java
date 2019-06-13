package com.ufoto.gateway.util;

import com.ufoto.gateway.contants.GatewayConstants;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.ipc.netty.http.HttpInfos;

/**
 * 日志工具类
 *
 * @author Luo Bao Ding
 * @since 2018/5/28
 */
public class LogUtil {

    /**
     * 日志内容生成
     *
     * @return 日志内容
     */
    public static String buildLogMsg(ServerWebExchange exchange) {
        Long startTime = exchange.getAttribute(GatewayConstants.FLT_ATT_KEY_ELAPSED_TIME_BEGIN);
        Long elapsedTime = null;
        if (startTime != null) {
            elapsedTime = System.currentTimeMillis() - startTime;
        }
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        String path = request.getURI().getPath();

        String ip = UserInfoUtil.getIpAddress(request);
        String httpVersion = ((HttpInfos) ((AbstractServerHttpRequest) exchange.getRequest()).getNativeRequest()).version().text();
        HttpStatus httpStatus = exchange.getResponse().getStatusCode();
        Object statusCode = httpStatus == null ? "null" : httpStatus.value();
        return ip +
                " " + method +
                " " + path +
                " " + httpVersion +
                " " + statusCode +
                " " + "-" +
                " " + elapsedTime;
    }
}
