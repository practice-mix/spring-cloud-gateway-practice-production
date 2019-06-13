package com.ufoto.gateway.contants;

/**
 * @author Luo Bao Ding
 * @since 2018/5/29
 */
public class GatewayConstants {
    //**************************//
    //******  filter attribute key  *//
    //*************************//
    public static final String FLT_ATT_KEY_ELAPSED_TIME_BEGIN = "elapsed_time_begin";

    //**************************//
    //******  http request key   *//
    //*************************//
    public static final String HTTP_REQ_KEY_SIGN = "sign";

    public static final String HTTP_REQ_KEY_UID = "uid";

    public static final String LOGIN_USER_KEY = "LOGIN_USER_KEY";


    //**************************//
    //******  super admin   *//
    //*************************//
    public static final String SUPER_ADMIN_MARK = "ufoto888888";


    //**************************//
    //******  redis   *********//
    //*************************//
    /**
     * 用户token key前缀  规则：U:T:用户id 如：用户id为：123 的 key为：U:T:123 value 对应 token值
     */
    public final static String REDIS_USER_TOKEN_KEY_ = "U:T:";

    /**
     * 社交独立用户token key前缀  规则：U:T:S:用户id 如：用户id为：123 的 key为：U:T:S:123 value 对应 token值
     */
    public final static String REDIS_SOCIAL_USER_TOKEN_KEY_ = "U:T:S:";

    /**
     * 用户token存活时间  7天, 单位：秒
     */
    public final static Long USER_TOKEN_TIME_LINE = 3600 * 24 * 7L;

    /**
     * redis set存储用户最近活跃 uid
     */
    public final static String REDIS_USER_ID_ACTIVE_SET_KEY = "REDIS_USER_ID_ACTIVE_SET_KEY";

    /**
     * redis set存储独立社交APP用户最近活跃 uid
     */
    public final static String REDIS_ONE_SOCIAL_USER_ID_ACTIVE_SET_KEY = "REDIS_ONE_SOCIAL_USER_ID_ACTIVE_SET_KEY";

    /**
     * redis set存储社交用户最近活跃 uid
     */
    public final static String REDIS_SOCIAL_USER_ID_ACTIVE_SET_KEY = "REDIS_SOCIAL_USER_ID_ACTIVE_SET_KEY";

    public static final String AUTH_KEY_UID = "uid";

    public static final String AUTH_KEY_SIGN = "sign";

    public static final String HTTP_METHOD_GET = "GET";
}
