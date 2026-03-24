package com.zjgsu.whattoeat.common.error;

public enum ErrorCode {
    // 用户相关
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),

    // 偏好/黑名单相关
    BLACKLIST_ALREADY_EXISTS(2001, "该餐厅已在黑名单中"),
    BLACKLIST_NOT_FOUND(2002, "黑名单记录不存在"),

    // 上游/集成相关
    AMAP_UPSTREAM_ERROR(3001, "高德地图服务异常"),
    AMAP_NO_RESULT(3002, "附近未找到符合条件的餐厅"),

    // 系统兜底
    SYSTEM_ERROR(9000, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
