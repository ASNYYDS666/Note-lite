package com.note.service.ai.facade;

/**
 * SSE 流式 token，携带 thinking 标记用于前端区分思考过程与正式回答。
 */
public record ChatToken(String text, boolean thinking) {

    public boolean isDone() {
        return "[DONE]".equals(text);
    }

    public static ChatToken think(String text) {
        return new ChatToken(text, true);
    }

    public static ChatToken answer(String text) {
        return new ChatToken(text, false);
    }

    public static final ChatToken DONE = new ChatToken("[DONE]", false);
}
