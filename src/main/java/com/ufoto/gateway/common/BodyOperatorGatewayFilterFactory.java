/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.ufoto.gateway.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.HttpMessageWriterResponse;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.FormHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Optional;

import static org.springframework.cloud.gateway.filter.factory.rewrite.RewriteUtils.process;

/**
 * origin from  ModifyRequestBodyGatewayFilterFactory
 * <p>
 * this class should not put to filter chain, is just for tool
 */
//@Component
public class BodyOperatorGatewayFilterFactory
        extends AbstractGatewayFilterFactory<BodyOperatorGatewayFilterFactory.Config> {
    private static final Logger LOGGER = LogManager.getLogger(BodyOperatorGatewayFilterFactory.class);

    private final ServerCodecConfigurer codecConfigurer;

    public BodyOperatorGatewayFilterFactory(ServerCodecConfigurer codecConfigurer) {
        super(Config.class);
        this.codecConfigurer = codecConfigurer;
        codecConfigurer.customCodecs().writer(new FormHttpMessageWriter());
    }

    @Override
    @SuppressWarnings("unchecked")
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ResolvableType bodyType = config.getBodyType();

            MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
            Optional<HttpMessageReader<?>> reader = RewriteUtils.getHttpMessageReader(codecConfigurer, bodyType, mediaType);

            if (reader.isPresent()) {
                Mono<Object> readMono = reader.get()
                        .readMono(bodyType, exchange.getRequest(), Collections.emptyMap())
                        .cast(Object.class);

                return process(readMono, peek -> {
                    Optional<HttpMessageWriter<?>> writer = RewriteUtils.getHttpMessageWriter(codecConfigurer, bodyType, mediaType);

                    if (writer.isPresent()) {
                        //[[[[[[[[ here control flow direction, defer from ModifyRequestBodyGatewayFilterFactory
                        Mono<Void> voidMono = config.rewriteFunction.apply(exchange, peek);
                        if (voidMono != null) {
                            return voidMono;
                        }
                        //]]]]]]]]]]]]]control flow direction

                        Publisher publisher = Mono.just(peek);
                        HttpMessageWriterResponse fakeResponse = new HttpMessageWriterResponse(exchange.getResponse().bufferFactory());
                        Mono write = writer.get().write(publisher, bodyType, mediaType,
                                fakeResponse, Collections.emptyMap());
//                        write.subscribe();
                        ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(
                                exchange.getRequest()) {
                            @Override
                            public HttpHeaders getHeaders() {
                                HttpHeaders httpHeaders = new HttpHeaders();
                                httpHeaders.putAll(super.getHeaders());
                                // httpbin.org
                                httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");//todo why must set HttpHeaders.TRANSFER_ENCODING chunked
                                /*if (fakeResponse.getHeaders().getContentType() != null) {
                                    httpHeaders.setContentType(
                                            fakeResponse.getHeaders().getContentType());
                                }*/
                                return httpHeaders;
                            }

                            @Override
                            public Flux<DataBuffer> getBody() {
                                return Flux.from(write.defaultIfEmpty("")).flatMap(o -> fakeResponse.getBody());
//                                return Flux.from(fakeResponse.getBody());

//                                return (Flux<DataBuffer>) fakeResponse.getBody();
//                                return Flux.from(write).flatMap(o -> fakeResponse.getBody());
//                                return (Flux<DataBuffer>) fakeResponse.getBody();
                            }
                        };
                        return chain.filter(exchange.mutate().request(decorator).build());
                    }
                    String message = "shouldn't run here, BodyOperatorGatewayFilterFactory.apply occur problem or http request is abnormal";
                    LOGGER.error(message, exchange::getRequest);
                    throw new RuntimeException(message);
                });

            }
            return chain.filter(exchange);
        };
    }

    public static class Config {
        private ResolvableType bodyType;

        private RewriteFunction<Object, Mono<Void>> rewriteFunction;

        public Config() {
        }

        public Config(ResolvableType bodyType, RewriteFunction<Object, Mono<Void>> rewriteFunction) {
            this.bodyType = bodyType;
            this.rewriteFunction = rewriteFunction;
        }

        public ResolvableType getBodyType() {
            return bodyType;
        }

        public Config setBodyType(ResolvableType bodyType) {
            this.bodyType = bodyType;
            return this;
        }

        public RewriteFunction getRewriteFunction() {
            return rewriteFunction;
        }

        public Config setRewriteFunction(RewriteFunction<Object, Mono<Void>> rewriteFunction) {
            this.rewriteFunction = rewriteFunction;
            return this;
        }
    }
}
