package com.ufoto.gateway.result.compatible;

import java.io.Serializable;

/**
 * 兼容app-api的结果对象
 *
 * @author Luo Bao Ding
 * @since 2018/5/11
 */
public class ApiResult implements Serializable {
    private static final long serialVersionUID = -2664983697620138297L;

    /**
     * 返回状态码
     */
    private Integer c;

    /**
     * 对应返回码说明
     */
    private String m;

    public ApiResult() {
    }

    public ApiResult(Integer c, String m) {
        this.c = c;
        this.m = m;
    }

    public Integer getC() {
        return c;
    }

    public void setC(Integer c) {
        this.c = c;
    }

    public String getM() {
        return m;
    }

    public void setM(String m) {
        this.m = m;
    }
}
