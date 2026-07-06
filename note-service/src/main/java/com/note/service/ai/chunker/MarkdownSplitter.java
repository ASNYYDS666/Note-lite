package com.note.service.ai.chunker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
public class MarkdownSplitter {

    private static final Pattern HEADER_PATTERN = Pattern.compile("\n(?=#{1,4} )");
    private static final Pattern CODE_FENCE = Pattern.compile("```");
    private static final Pattern CODE_TAG = Pattern.compile("</?pre>|</?code>");
    private static final String[] PUNCTUATIONS = {". ", "！", "？", "；", "，", ". ", "! ", "? "};

    private final int chunkSize;
    private final int overlapSize;

    public MarkdownSplitter(@Value("${note.chunk.size:500}") int chunkSize,
                            @Value("${note.chunk.overlap:100}") int overlapSize) {
        this.chunkSize = chunkSize;
        this.overlapSize = overlapSize;
    }

    public List<Chunk> chunk(String content, Map<String, Object> metadata) {
        List<Chunk> chunks = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return chunks;
        }

        // Split by H1-H4 headers for semantic boundary preservation
        String[] sections = HEADER_PATTERN.split(content);
        int index = 0;

        for (String section : sections) {
            if (section.trim().isEmpty()) continue;

            if (section.length() > chunkSize) {
                chunks.addAll(splitByWindow(section, metadata, index));
                index += Math.max(1, (int) Math.ceil((double) section.length() / (chunkSize - overlapSize)));
            } else {
                Chunk chunk = new Chunk();
                chunk.setIndex(index++);
                chunk.setText(section.trim());
                chunk.setMetadata(new LinkedHashMap<>(metadata));
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    private List<Chunk> splitByWindow(String text, Map<String, Object> metadata, int baseIndex) {
        List<Chunk> result = new ArrayList<>();
        int start = 0;
        int minChunkSize = Math.max(50, chunkSize / 10);

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // 到达末尾，直接截取最后一块并退出
            if (end >= text.length()) {
                Chunk chunk = new Chunk();
                chunk.setIndex(baseIndex + result.size());
                chunk.setText(text.substring(start).trim());
                chunk.setMetadata(new LinkedHashMap<>(metadata));
                result.add(chunk);
                break;
            }

            int breakPoint = findBreakPoint(text, start, end);
            if (breakPoint > start + minChunkSize) {
                end = breakPoint + 1;
            }

            Chunk chunk = new Chunk();
            chunk.setIndex(baseIndex + result.size());
            chunk.setText(text.substring(start, end).trim());
            chunk.setMetadata(new LinkedHashMap<>(metadata));
            result.add(chunk);

            start = end - overlapSize;
        }

        return result;
    }

    /**
     * Find a safe break point in {@code text} between {@code start} and {@code end},
     * adjusted to avoid splitting inside code blocks.
     */
    private int findBreakPoint(String text, int start, int end) {
        // Priority 0: avoid splitting inside a code block (``` or <pre><code>)
        int codeAdjusted = adjustForCodeBlock(text, start, end);
        if (codeAdjusted > start + 50) {
            return codeAdjusted;
        }

        // Priority 1: paragraph break (double newline)
        int bp = text.lastIndexOf("\n\n", end);
        if (bp > start + chunkSize / 3) return bp;

        // Priority 2: Chinese sentence end
        bp = text.lastIndexOf('。', end);
        if (bp > start + chunkSize / 2) return bp;

        // Priority 3: line break
        bp = text.lastIndexOf('\n', end);
        if (bp > start + chunkSize / 2) return bp;

        // Priority 4: any punctuation
        for (String punct : PUNCTUATIONS) {
            bp = text.lastIndexOf(punct, end);
            if (bp > start + chunkSize / 2) return bp;
        }

        return -1;
    }

    /**
     * If {@code end} falls inside a code block, back up to before the code block.
     * If the code block covers most of this chunk, split at the nearest newline
     * inside the code block instead.
     */
    private int adjustForCodeBlock(String text, int start, int end) {
        int minChunk = Math.max(50, chunkSize / 10);

        // --- Markdown ``` fences ---
        int fenceStart = text.lastIndexOf("```", end);
        if (fenceStart >= start) {
            int closingFence = text.indexOf("```", fenceStart + 3);
            boolean inside = (closingFence == -1 || closingFence > end);
            if (inside) {
                if (fenceStart > start + chunkSize / 3) {
                    return fenceStart - 1;
                }
                int nl = text.lastIndexOf('\n', end);
                if (nl > start + minChunk) return nl;
            }
        }

        // --- HTML <pre><code> blocks ---
        int tagStart = text.lastIndexOf("<pre>", end);
        if (tagStart < 0) tagStart = text.lastIndexOf("<pre><code>", end);
        if (tagStart >= start) {
            int tagEnd = text.indexOf("</pre>", tagStart + 5);
            boolean inside = (tagEnd == -1 || tagEnd > end);
            if (inside) {
                if (tagStart > start + chunkSize / 3) {
                    return tagStart - 1;
                }
                int nl = text.lastIndexOf('\n', end);
                if (nl > start + minChunk) return nl;
            }
        }

        return -1;
    }
}
