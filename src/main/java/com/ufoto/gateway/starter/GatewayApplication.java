package com.ufoto.gateway.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 启动入口
 *
 * Created by LuoBaoding on 2018/5/7
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = "com.ufoto.gateway")
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
