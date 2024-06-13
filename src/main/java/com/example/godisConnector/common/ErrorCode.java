package com.example.godisConnector.common;

public enum ErrorCode {

    SUCCESS(200, "Success"),
    PARAMS_ERROR(40000, "请求参数错误"),
    COMMAND_UNKNOWN(40100, "未知命令"),

    GET_NIL(40101, "未知命令"),
    UNKNOWN_ERR(40102, "未知错误");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}