package com.ufoto.gateway.enums;

/**
 * 网关系统的错误码
 * <p>
 * Created by LuoBaoding on 2018/5/7
 */
public enum GatewayErrorCodeEnum {
    /**
     * app-api 熔断
     */
    APP_API_CIRCUIT_BREAKER("0701001001", "app-api 熔断"),

    //**********************//
    //******* 老错误码，保留作兼容  ***//
    //********************//
    TOKEN_INVALID_EXCEPTION("301", "token失效"),

    PARAM_ILLEGAL_EXCEPTION("302", "参数异常"),

    SYSTEM_ERROR("500", "token失效"),;


    /**
     * 错误码
     */
    private String errorCode;
    /**
     * 错误码描述信息
     */
    private String description;

    GatewayErrorCodeEnum(String errorCode, String description) {
        this.errorCode = errorCode;
        this.description = description;
    }

    public String getErrorCode() {
        return errorCode;
    }


    public String getDescription() {
        return description;
    }

}
