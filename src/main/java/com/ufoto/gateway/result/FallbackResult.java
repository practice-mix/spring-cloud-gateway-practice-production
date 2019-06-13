package com.ufoto.gateway.result;

import java.io.Serializable;

/**
 * 熔断后的反回结果
 *
 * Created by LuoBaoding on 2018/5/7
 */
public class FallbackResult implements Serializable {
    private static final long serialVersionUID = -5727579013183890686L;

    private String errorCode;

    private String msg;

    public FallbackResult() {
    }

    public FallbackResult(String errorCode, String msg) {
        this.errorCode = errorCode;
        this.msg = msg;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
