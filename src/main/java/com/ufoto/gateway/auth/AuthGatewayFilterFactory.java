package com.ufoto.gateway.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.ufoto.gateway.auth.config.LoginAuthConfig;
import com.ufoto.gateway.common.BaseFilter;
import com.ufoto.gateway.common.BodyOperatorGatewayFilterFactory;
import com.ufoto.gateway.contants.GatewayConstants;
import com.ufoto.gateway.enums.GatewayErrorCodeEnum;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 鉴权过滤器
 *
 * @author Luo Bao Ding
 * @since 2018/5/29
 */
//@Component
public class AuthGatewayFilterFactory extends BaseFilter {
    private static final Logger LOGGER = LogManager.getLogger(AuthGatewayFilterFactory.class);

    private LoginAuthConfig loginAuthConfig;

    private final BodyOperatorGatewayFilterFactory bodyOpsFilterFactory;

    @Autowired
    public AuthGatewayFilterFactory(LoginAuthConfig loginAuthConfig, BodyOperatorGatewayFilterFactory bodyOpsFilterFactory) {
        this.loginAuthConfig = loginAuthConfig;
        this.bodyOpsFilterFactory = bodyOpsFilterFactory;
    }

    @Override
    public GatewayFilter apply(Object config) {

        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            //判断是否需要鉴权
            String path = request.getURI().getPath();
            if (!loginAuthConfig.isRequireAuth(path)) {
                return chain.filter(exchange);
            }

            //先探一下header或url中是否有auth相关key，有则走同步
            Mono<Void> result = tryAuthInSync(exchange, chain);
            if (result != null) {
                return result;
            }

            String methodValue = request.getMethodValue();
            if (GatewayConstants.HTTP_METHOD_GET.equalsIgnoreCase(methodValue)) {
                //synchronously
                return authInSync(exchange, chain);
            }
            // post request
            else {
                return authInAsync(exchange, chain);
            }

        };
    }

    private Mono<Void> tryAuthInSync(ServerWebExchange exchange, GatewayFilterChain chain) {
        String uid = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_UID);
        String sign = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_SIGN);
        if (StringUtils.isNotBlank(uid) && StringUtils.isNotBlank(sign)) {
            return auth(exchange, chain, uid, sign);
        }
        return null;
    }

    private Mono<Void> authInAsync(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        MediaType contentType = request.getHeaders().getContentType();
        assert contentType != null;
        //Content-Type: application/x-www-form-urlencoded
        if ("application".equalsIgnoreCase(contentType.getType())
                && "x-www-form-urlencoded".equalsIgnoreCase(contentType.getSubtype())) {
            return buildAsyncAuthRstOnUrlencoded(exchange, chain);
        }
        //Content-Type: application/json
        else if ("application".equalsIgnoreCase(contentType.getType())
                && "json".equalsIgnoreCase(contentType.getSubtype())) {
            //asynchronously read post request body
            return buildAsyncAuthRstOnJson(exchange, chain);

        }
        //Content-Type: multipart/form-data
        else if ("multipart".equalsIgnoreCase(contentType.getType())
                && "form-data".equalsIgnoreCase(contentType.getSubtype())) {
            //asynchronously read post request body
            return buildAsyncAuthRstOnMultipart(exchange, chain);

        }
        // TODO: 2018/6/11 添加支持multipart/form-data
        else {
            String message = "only support post: Content-Type : application/x-www-form-urlencoded, application/json . actual content type is : " + contentType;
            LOGGER.error(message);
            throw new UnsupportedOperationException(message);
        }
    }

    private Mono<Void> buildAsyncAuthRstOnMultipart(ServerWebExchange exchange, GatewayFilterChain chain) {

        return bodyOpsFilterFactory.apply(new BodyOperatorGatewayFilterFactory.Config(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class), this::authFnOnMultipart
        )).filter(exchange, chain);

    }
    private Mono<Void> buildAsyncAuthRstOnJson(ServerWebExchange exchange, GatewayFilterChain chain) {

        return bodyOpsFilterFactory.apply(new BodyOperatorGatewayFilterFactory.Config(ResolvableType.forClass(JsonNode.class), this::authFnOnJson
        )).filter(exchange, chain);

    }

    private Mono<Void> authFnOnMultipart(ServerWebExchange exchange, Object originalBody) {
        @SuppressWarnings("unchecked")
        MultiValueMap<String, Part> multiValueMap = (MultiValueMap<String, Part>) originalBody;

        Part uidPart = null;
        Part signPart = null;
        String uid = null;
        String sign = null;

        if (multiValueMap != null) {
            uidPart = multiValueMap.getFirst(GatewayConstants.AUTH_KEY_UID);
            signPart = multiValueMap.getFirst(GatewayConstants.AUTH_KEY_SIGN);
        }
        Flux<DataBuffer> uidContent = uidPart.content();
        Flux<DataBuffer> signContent = signPart.content();

        // TODO: 2018/6/11


        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("multipart/form-data : auth sign = " + sign);
            LOGGER.debug("multipart/form-data : auth uid = " + uid);
        }
        return auth(exchange, uid, sign, () -> null);
    }
    private Mono<Void> authFnOnUrlencoded(ServerWebExchange exchange, Object originalBody) {
        @SuppressWarnings("unchecked")
        MultiValueMap<String, String> multiValueMap = (MultiValueMap<String, String>) originalBody;

        String uid = null;
        String sign = null;

        if (multiValueMap != null) {
            uid = multiValueMap.getFirst(GatewayConstants.AUTH_KEY_UID);
            sign = multiValueMap.getFirst(GatewayConstants.AUTH_KEY_SIGN);
        }

        //应对客户端不规范传值情形, 即uid不同时放在一处(header,url,body)
        if (StringUtils.isBlank(uid)) {
            uid = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_UID);
        }
        if (StringUtils.isBlank(sign)) {
            sign = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_SIGN);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("application/x-www-form-urlencoded : auth sign = " + sign);
            LOGGER.debug("application/x-www-form-urlencoded : auth uid = " + uid);
        }
        return auth(exchange, uid, sign, () -> null);
    }

    private Mono<Void> authFnOnJson(ServerWebExchange exchange, Object originalBody) {
        JsonNode jsonNode = (JsonNode) originalBody;
        String uid = jsonNode.get("uid").asText();
        String sign = jsonNode.get("sign").asText();
        //应对客户端不规范传值情形
        if (StringUtils.isBlank(uid)) {
            uid = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_UID);
        }
        if (StringUtils.isBlank(sign)) {
            sign = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_SIGN);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("application/json : auth sign = " + sign);
            LOGGER.debug("application/json : auth uid = " + uid);
        }
        return auth(exchange, uid, sign, () -> null);
    }

    private Mono<Void> buildAsyncAuthRstOnUrlencoded(ServerWebExchange exchange, GatewayFilterChain chain) {

        return bodyOpsFilterFactory.apply(new BodyOperatorGatewayFilterFactory.Config(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class), this::authFnOnUrlencoded
        )).filter(exchange, chain);

    }

    private Mono<Void> authInSync(ServerWebExchange exchange, GatewayFilterChain chain) {
      /*  AbstractServerHttpResponse response = (AbstractServerHttpResponse) exchange.getResponse();
        AbstractServerHttpRequest request = (AbstractServerHttpRequest) exchange.getRequest();
*/
//        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        try {
            String uid = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_UID);
            String sign = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_SIGN);

            return auth(exchange, chain, uid, sign);

        } catch (Exception e) {
            e.printStackTrace();
            return filterSupport.buildResult(response, GatewayErrorCodeEnum.SYSTEM_ERROR, "token失效，请重新登录");
        }

    }

    /**
     * 同步下使用
     */
    private Mono<Void> auth(ServerWebExchange exchange, GatewayFilterChain chain, String uid, String sign) {
        return auth(exchange, uid, sign, () -> chain.filter(exchange));
    }

    /**
     * 通过则执行 @param supplier
     */
    private Mono<Void> auth(ServerWebExchange exchange, String uid, String sign, Supplier<Mono<Void>> supplier) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        //判断超管
        if (isSuperAdmin(sign)) {
            return supplier.get();
        }
        //检查参数
        if (StringUtils.isBlank(sign)) {
            return filterSupport.buildResult(response, GatewayErrorCodeEnum.PARAM_ILLEGAL_EXCEPTION, "sign不能为空");
        }
        if (StringUtils.isBlank(uid)) {
            return filterSupport.buildResult(response, GatewayErrorCodeEnum.PARAM_ILLEGAL_EXCEPTION, "uid不能为空");
        }

        //检查token
        String token = redisTpl.opsForValue().get(GatewayConstants.REDIS_USER_TOKEN_KEY_ + uid);
        String socialToken = redisTpl.opsForValue().get(GatewayConstants.REDIS_SOCIAL_USER_TOKEN_KEY_ + uid);

        if (token == null && socialToken == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("用户" + uid + "的token不存在");
            }
            return filterSupport.buildResult(response, GatewayErrorCodeEnum.TOKEN_INVALID_EXCEPTION, "token不存在，请重新登录");
        }
        String uri = request.getURI().getPath();
        if (!filterSupport.checkTokenBySign(uri, token, sign) && !filterSupport.checkTokenBySign(uri, socialToken, sign)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("用户" + uid + "的token与sign不一致");
            }
            return filterSupport.buildResult(response, GatewayErrorCodeEnum.TOKEN_INVALID_EXCEPTION, "token失效，请重新登录");
        }

        //刷新token
        refreshToken(GatewayConstants.REDIS_USER_TOKEN_KEY_ + uid, token);
        refreshToken(GatewayConstants.REDIS_SOCIAL_USER_TOKEN_KEY_ + uid, socialToken);
        return supplier.get();
    }

    private void refreshToken(String key, String token) {
        if (token != null) {
            redisTpl.opsForValue().set(key, token, GatewayConstants.USER_TOKEN_TIME_LINE, TimeUnit.SECONDS);
        }
    }


    private boolean isSuperAdmin(String sign) {
        return GatewayConstants.SUPER_ADMIN_MARK.equals(sign);
    }

}