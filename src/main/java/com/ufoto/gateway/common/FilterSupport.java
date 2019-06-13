package com.ufoto.gateway.common;

import com.alibaba.fastjson.JSONObject;
import com.ufoto.gateway.enums.GatewayErrorCodeEnum;
import com.ufoto.gateway.result.compatible.ApiResult;
import com.ufoto.gateway.util.EncryptKeyUtil;
import org.apache.commons.lang.StringUtils;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author Luo Bao Ding
 * @since 2018/5/31
 */
@Component
public class FilterSupport {

    public Mono<Void> buildResult(ServerHttpResponse response, GatewayErrorCodeEnum paramIllegalException, String msg) {
        ApiResult apiResult = new ApiResult(Integer.valueOf(paramIllegalException.getErrorCode()),
                msg);
        byte[] bytes = JSONObject.toJSONBytes(apiResult);
        DataBuffer dataBuffer = response.bufferFactory().wrap(bytes);
        Publisher<? extends Publisher<? extends DataBuffer>> body = Mono.just(Mono.just(dataBuffer));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        return response.writeAndFlushWith(body);
    }

    public Boolean checkTokenBySign(String uri, String token, String sign) {
        if (StringUtils.isBlank(token) || StringUtils.isBlank(sign)) {
            return false;
        }
        // 与后台查出来的token md5加密比较 相等 通过
        String md5S = uri + "?token=" + token;
        return sign.equals(EncryptKeyUtil.getMD5(md5S));
    }

    /**
     * support http method : get
     */
    public String extractByKey(ServerWebExchange exchange, String key) {
//        AbstractServerHttpRequest request = (AbstractServerHttpRequest) exchange.getRequest();
        ServerHttpRequest request = exchange.getRequest();
        String val = request.getHeaders().getFirst(key);
        if (StringUtils.isNotBlank(val)) {
            return val;
        }
        val = request.getQueryParams().getFirst(key);
        return val;
    }

}
