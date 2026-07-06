package com.note.service.common.constant;

/**
 * 缓存 Key 前缀与 TTL 统一常量
 * 命名规范：{模块}:{操作}:{标识}
 */
public final class CacheConstants {

    private CacheConstants() {}

    // ========== Key 前缀 ==========

    /** 笔记详情缓存（认证用户维度），格式：note:detail:{userId}:{noteId} */
    public static final String NOTE_DETAIL_PREFIX = "note:detail:";

    /** 分享笔记详情缓存（无认证），格式：note:detail:share:{noteId} */
    public static final String NOTE_SHARE_DETAIL_PREFIX = "note:detail:share:";

    /** 笔记草稿缓存，格式：note:draft:{userId}:{noteId}|new */
    public static final String NOTE_DRAFT_PREFIX = "note:draft:";

    /** 分享码缓存，格式：share:code:{code} */
    public static final String SHARE_CODE_PREFIX = "share:code:";

    /** 空值缓存（防穿透），格式：note:null:{noteId} */
    public static final String NOTE_NULL_PREFIX = "note:null:";

    /** 分享码空值缓存（防穿透），格式：share:code:null:{code} */
    public static final String SHARE_CODE_NULL_PREFIX = "share:code:null:";

    /** 笔记树缓存，格式：note:tree:{userId} */
    public static final String NOTE_TREE_PREFIX = "note:tree:";

    // ========== TTL（单位：秒） ==========

    /** 笔记详情 TTL：6 小时 */
    public static final long NOTE_DETAIL_TTL_SECONDS = 6 * 3600;

    /** 空值缓存 TTL：60 秒（防穿透） */
    public static final long NOTE_NULL_TTL_SECONDS = 60;

    /** 分享码空值 TTL：120 秒（防穿透） */
    public static final long SHARE_CODE_NULL_TTL_SECONDS = 120;

    /** 草稿 TTL：3 天 */
    public static final long NOTE_DRAFT_TTL_SECONDS = 3 * 86400;

    /** 分享码 TTL：7 天（与数据库 expire_at 对齐） */
    public static final long SHARE_CODE_TTL_SECONDS = 7 * 86400;

    /** 笔记树 TTL：10 秒 */
    public static final long NOTE_TREE_TTL_SECONDS = 10;

    // ========== 工具方法 ==========

    /** TTL 随机偏移比例：±15% */
    private static final double TTL_JITTER_RATIO = 0.15;

    /**
     * 返回带 ±15% 随机偏移的 TTL（防雪崩）
     * 例：基值 21600s → 返回 18360~24840s 之间的随机值
     * @param baseSeconds TTL 基准值（秒）
     * @return 带随机偏移后的 TTL（秒）
     */
    public static long ttlWithJitter(long baseSeconds) {
        double jitter = 1.0 + (Math.random() * 2 - 1) * TTL_JITTER_RATIO;
        return (long) (baseSeconds * jitter);
    }
}
