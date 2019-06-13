package com.ufoto.gateway.stat;

import com.fasterxml.jackson.databind.JsonNode;
import com.ufoto.gateway.common.BaseFilter;
import com.ufoto.gateway.common.BodyOperatorGatewayFilterFactory;
import com.ufoto.gateway.contants.GatewayConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 统计过滤器
 *
 * @author Luo Bao Ding
 * @since 2018/5/30
 */
@ConfigurationProperties(prefix = "social-active-stat")
//@Component
public class ActiveStatGatewayFilterFactory extends BaseFilter {
    private static final Logger LOGGER = LogManager.getLogger(ActiveStatGatewayFilterFactory.class);

    private List<String> uriPrefixes = new ArrayList<>();

    private final Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();

    private final BodyOperatorGatewayFilterFactory bodyOpsFilterFactory;

    public ActiveStatGatewayFilterFactory(BodyOperatorGatewayFilterFactory bodyOpsFilterFactory) {
        this.bodyOpsFilterFactory = bodyOpsFilterFactory;
    }


    @Override
    public GatewayFilter apply(Object config) {

        //todo handle the experiment
/* return chain.filter(exchange).then(Mono.defer(() -> {
     return buildEventualStatResult(exchange, chain);
 }));*/
        return this::buildEventualStatResult;
    }

    private Mono<Void> buildEventualStatResult(ServerWebExchange exchange, GatewayFilterChain chain) {
//        AbstractServerHttpRequest request = (AbstractServerHttpRequest) exchange.getRequest();
        ServerHttpRequest request = exchange.getRequest();
        String uri = request.getURI().getPath();

        //排除登录接口（显然无uid）
        if (StringUtils.isNotBlank(uri) &&
                (uri.startsWith("/userApi/postData") || uri.startsWith("/tool/unsafe/sign/trySign"))) {
            return chain.filter(exchange);
        }
        //先探一下header或url中是否有auth相关key，有则走同步
        Mono<Void> result = tryStatInSync(exchange, uri, chain);
        if (result != null) {
            return result;
        }

        String methodValue = request.getMethodValue();
        if (GatewayConstants.HTTP_METHOD_GET.equalsIgnoreCase(methodValue)) {
            //synchronously
            return statInSync(exchange, chain, uri);
        }
        // post request
        else {
            return statInAsync(exchange, chain);
        }
    }

    private Mono<Void> tryStatInSync(ServerWebExchange exchange, String uri, GatewayFilterChain chain) {
        String uid = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_UID);
        String sign = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_SIGN);
        if (StringUtils.isNotBlank(uid) && StringUtils.isNotBlank(sign)) {
            return stat(exchange, chain, uid, sign);
        }
        return null;
    }

    private Mono<Void> statInSync(ServerWebExchange exchange, GatewayFilterChain chain, String uri) {
        String uid = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_UID);
        String sign = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_SIGN);
        return stat(exchange, chain, uid, sign);
    }

    private Mono<Void> statInAsync(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        MediaType contentType = request.getHeaders().getContentType();
        assert contentType != null;
        //Content-Type: application/x-www-form-urlencoded
        if ("application".equalsIgnoreCase(contentType.getType())
                && "x-www-form-urlencoded".equalsIgnoreCase(contentType.getSubtype())) {
            return buildAsyncStatRstOnUrlencoded(exchange, chain);
        }
        //Content-Type: application/json
        else if ("application".equalsIgnoreCase(contentType.getType())
                && "json".equalsIgnoreCase(contentType.getSubtype())) {
            //asynchronously read post request body
            return buildAsyncStatRstOnJson(exchange, chain);

        } else {
            String message = "only support post: Content-Type : application/x-www-form-urlencoded, application/json . actual content type is : " + contentType;
            LOGGER.error(message);
            throw new UnsupportedOperationException(message);
        }
    }

    //todo
    private Mono<Void> buildAsyncStatRstOnJson(ServerWebExchange exchange, GatewayFilterChain chain) {
        return bodyOpsFilterFactory.apply(new BodyOperatorGatewayFilterFactory.Config(ResolvableType.forClass(JsonNode.class),
                this::statFnOnJson
        )).filter(exchange, chain);

//        =============================
//        AbstractServerHttpRequest request = (AbstractServerHttpRequest) exchange.getRequest();
/*        ServerHttpRequest request = exchange.getRequest();

        Flux<DataBuffer> body = request.getBody();

        return decoder.decode(body, ResolvableType.forClass(JsonNode.class), MediaType.APPLICATION_JSON_UTF8, null)
                .flatMap(o -> {
                    JsonNode jsonNode = (JsonNode) o;
                    String uid = jsonNode.get(GatewayConstants.AUTH_KEY_UID).asText();
                    String sign = jsonNode.get(GatewayConstants.AUTH_KEY_SIGN).asText();
                    //应对客户端不规范传值情形
                    if (StringUtils.isBlank(uid)) {
                        uid = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_UID);
                    }
                    if (StringUtils.isBlank(sign)) {
                        sign = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_SIGN);
                    }
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("application/json : stat sign = " + sign);
                        LOGGER.debug("application/json : stat uid = " + uid);
                    }

                    return stat(uri, uid, sign);
                }).next();*/
    }

    private Mono<Void> buildAsyncStatRstOnUrlencoded(ServerWebExchange exchange, GatewayFilterChain chain) {
        return bodyOpsFilterFactory.apply(new BodyOperatorGatewayFilterFactory.Config(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
                this::statFnOnUrlencoded
        )).filter(exchange, chain);

//       =====================
       /*
                   String uri = request.getURI().getPath();
       Mono<MultiValueMap<String, String>> formDataMono = exchange.getFormData();
        return formDataMono.flatMap(multiValueMap -> {
            String uid = multiValueMap.getFirst(GatewayConstants.AUTH_KEY_UID);
            String sign = multiValueMap.getFirst(GatewayConstants.AUTH_KEY_SIGN);
            //应对客户端不规范传值情形
            if (StringUtils.isBlank(uid)) {
                uid = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_UID);
            }
            if (StringUtils.isBlank(sign)) {
                sign = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_SIGN);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("application/x-www-form-urlencoded : stat sign = " + sign);
                LOGGER.debug("application/x-www-form-urlencoded : stat uid = " + uid);
            }
            return stat(uri, uid, sign);
        });*/
    }

    private Mono<Void> statFnOnJson(ServerWebExchange exchange, Object originalBody) {
        String uri = exchange.getRequest().getURI().getPath();

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
            LOGGER.debug("application/json : stat sign = " + sign);
            LOGGER.debug("application/json : stat uid = " + uid);
        }
        return stat(uri, uid, sign, () -> null /*Mono::empty*/);// TODO: 2018/6/9  需要确认是只需要返回Mono::empty

    }

    private Mono<Void> statFnOnUrlencoded(ServerWebExchange exchange, Object o) {
        String uri = exchange.getRequest().getURI().getPath();
        @SuppressWarnings("unchecked")
        MultiValueMap<String, String> multiValueMap = (MultiValueMap<String, String>) o;

        String uid = null;
        String sign = null;

        if (multiValueMap != null) {
            uid = multiValueMap.getFirst(GatewayConstants.AUTH_KEY_UID);
            sign = multiValueMap.getFirst(GatewayConstants.AUTH_KEY_SIGN);
        }
        //应对客户端不规范传值情形
        if (StringUtils.isBlank(uid)) {
            uid = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_UID);
        }
        if (StringUtils.isBlank(sign)) {
            sign = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_SIGN);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("application/x-www-form-urlencoded : stat sign = " + sign);
            LOGGER.debug("application/x-www-form-urlencoded : stat uid = " + uid);
        }
        return stat(uri, uid, sign, () -> null /*Mono::empty*/);// TODO: 2018/6/9  需要确认是只需要返回Mono::empty

//        ========================
 /*       String uri = exchange.getRequest().getURI().getPath();
        Mono<MultiValueMap<String, String>> formDataMono = exchange.getFormData();

        return formDataMono.flatMap(multiValueMap -> {
            String uid = multiValueMap.getFirst(GatewayConstants.AUTH_KEY_UID);
            String sign = multiValueMap.getFirst(GatewayConstants.AUTH_KEY_SIGN);
            //应对客户端不规范传值情形
            if (StringUtils.isBlank(uid)) {
                uid = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_UID);
            }
            if (StringUtils.isBlank(sign)) {
                sign = filterSupport.extractByKey(exchange, GatewayConstants.HTTP_REQ_KEY_SIGN);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("application/x-www-form-urlencoded : stat sign = " + sign);
                LOGGER.debug("application/x-www-form-urlencoded : stat uid = " + uid);
            }
            return stat(uri, uid, sign,Mono::empty);// TODO: 2018/6/9  需要确认是只返回Mono::empty
        });*/
    }

    /**
     * 同步下使用
     */
    private Mono<Void> stat(ServerWebExchange exchange, GatewayFilterChain chain, String uid, String sign) {
        String uri = exchange.getRequest().getURI().getPath();
        return stat(uri, uid, sign, () -> chain.filter(exchange));
    }

    private Mono<Void> stat(String uri, String uid, String sign, Supplier<Mono<Void>> supplier) {

        if (StringUtils.isNotBlank(uid)) {
            //保存用户活跃时间
            long curTimeInSec = System.currentTimeMillis() / 1000;
            redisTpl.opsForZSet().add(GatewayConstants.REDIS_USER_ID_ACTIVE_SET_KEY, uid, curTimeInSec);

            //社交统计
            for (String prefix : uriPrefixes) {
                if (uri.startsWith(prefix)) {
                    String socialToken = redisTpl.opsForValue().get(GatewayConstants.REDIS_SOCIAL_USER_TOKEN_KEY_ + uid);
                    //独立社交app活跃用户
                    if (filterSupport.checkTokenBySign(uri, socialToken, sign)) {
                        redisTpl.opsForZSet().add(GatewayConstants.REDIS_ONE_SOCIAL_USER_ID_ACTIVE_SET_KEY, uid, curTimeInSec);
                    }
                    //社交用户活跃
                    else {
                        redisTpl.opsForZSet().add(GatewayConstants.REDIS_SOCIAL_USER_ID_ACTIVE_SET_KEY, uid, curTimeInSec);
                    }
                    break;
                }
            }
        }
        return supplier.get();
    }

    public List<String> getUriPrefixes() {
        return uriPrefixes;
    }

    public void setUriPrefixes(List<String> uriPrefixes) {
        this.uriPrefixes = uriPrefixes;
    }

}
