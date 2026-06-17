package com.note.service.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // ========== 用户模块 100xx ==========
    USERNAME_EXISTS(10001, "用户名已存在"),
    EMAIL_EXISTS(10002, "邮箱已被注册"),
    LOGIN_FAILED(10003, "用户名或密码错误"),
    TOKEN_EXPIRED(10004, "Token 已过期"),
    TOKEN_INVALID(10005, "Token 无效"),

    // ========== 笔记模块 200xx ==========
    NOTE_NOT_FOUND(20001, "笔记不存在"),
    NOTE_NO_PERMISSION(20002, "无权操作该笔记"),
    NOTE_IN_RECYCLE(20003, "笔记已在回收站"),
    NOTE_NOT_IN_RECYCLE(20004, "笔记不在回收站"),

    // ========== 分享模块 300xx ==========
    SHARE_NOTE_NOT_FOUND(30001, "分享的笔记不存在"),
    SHARE_CODE_INVALID(30002, "分享码不存在或已过期"),
    SHARE_GENERATE_FAILED(30003, "生成分享码失败，请重试"),

    // ========== AI 模块 500xx ==========
    AI_CONFIG_NOT_FOUND(50001, "请先在 AI 设置中配置 API Key"),
    AI_CONFIG_DISABLED(50002, "AI 服务已被禁用，请前往 AI 设置开启"),
    AI_API_KEY_INVALID(50003, "API Key 无效，请检查 AI 设置"),
    AI_EMBEDDING_FAILED(50004, "向量化失败，请稍后重试"),
    AI_CHAT_FAILED(50005, "AI 对话服务暂不可用，请稍后重试"),
    AI_VECTOR_STORE_ERROR(50006, "知识库检索服务暂不可用"),
    AI_RATE_LIMITED(50007, "请求频率过高，请稍后重试"),
    AI_PROVIDER_UNSUPPORTED(50008, "不支持该 AI 服务商"),

    // ========== 系统通用 400xx ==========
    PARAM_VALIDATION_FAILED(40001, "参数校验失败"),
    SYSTEM_ERROR(40002, "系统繁忙，请稍后重试");

    private final Integer code;
    private final String defaultMessage;

    ErrorCode(Integer code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
