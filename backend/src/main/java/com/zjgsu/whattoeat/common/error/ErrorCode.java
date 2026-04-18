package com.zjgsu.whattoeat.common.error;

public enum ErrorCode {
    // 用户与鉴权相关
    VALIDATION_FAILED(1001, "参数校验失败"),
    USER_NOT_FOUND(1002, "用户不存在"),
    UNAUTHORIZED(1003, "未登录或token无效"),
    LOGIN_CODE_INVALID(1004, "登录code非法"),

    // 黑名单与备注相关
    BLACKLIST_ALREADY_EXISTS(2001, "重复拉黑"),
    BLACKLIST_NOT_FOUND(2002, "黑名单记录不存在"),
    NOTE_CONTENT_INVALID(2003, "备注内容非法"),
    NOTE_NOT_FOUND(2004, "备注不存在"),
    NOTE_ALREADY_EXISTS(2005, "备注已存在"),
    REVIEW_CONTENT_INVALID(2101, "评论内容非法"),
    REVIEW_RATING_INVALID(2102, "评分非法"),
    REVIEW_PRICE_INVALID(2103, "人均价格非法"),
    REVIEW_NOT_FOUND(2104, "评论不存在"),

    // 上游/集成相关
    AMAP_UPSTREAM_ERROR(3001, "高德上游失败"),
    AMAP_UPSTREAM_TIMEOUT(3002, "高德上游超时"),
    AMAP_NO_RESULT(3003, "高德无结果"),
    AI_UPSTREAM_ERROR(3004, "AI 服务失败"),
    AI_UPSTREAM_TIMEOUT(3005, "AI 服务超时"),

    // 系统兜底
    SYSTEM_ERROR(9000, "系统异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
