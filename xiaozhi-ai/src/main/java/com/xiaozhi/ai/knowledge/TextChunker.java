package com.xiaozhi.ai.knowledge;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档切片：按段落聚合到目标长度，超长段落硬切，相邻分片保留一小段重叠。
 * <p>
 * 重叠是为了避免一句话被切断后两边都检索不到；切片长度按字符算，中文按字、英文按字母，
 * 对嵌入模型来说都是 token 量级的近似。
 */
public final class TextChunker {

    public static final int DEFAULT_MAX_CHARS = 500;
    public static final int DEFAULT_OVERLAP = 50;

    private TextChunker() {
    }

    public static List<String> chunk(String text) {
        return chunk(text, DEFAULT_MAX_CHARS, DEFAULT_OVERLAP);
    }

    public static List<String> chunk(String text, int maxChars, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null) {
            return chunks;
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.isEmpty()) {
            return chunks;
        }
        int max = Math.max(50, maxChars);
        int overlapSize = Math.max(0, Math.min(overlap, max / 2));

        StringBuilder current = new StringBuilder();
        for (String paragraph : normalized.split("\n{2,}")) {
            String block = paragraph.trim();
            if (block.isEmpty()) {
                continue;
            }
            // 单个段落就超长时先冲刷已有内容，再硬切这一段
            if (block.length() > max) {
                flush(chunks, current);
                splitLongBlock(chunks, block, max, overlapSize);
                continue;
            }
            if (current.length() + block.length() + 1 > max) {
                flush(chunks, current);
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(block);
        }
        flush(chunks, current);
        return chunks;
    }

    private static void splitLongBlock(List<String> chunks, String block, int max, int overlapSize) {
        int start = 0;
        while (start < block.length()) {
            int end = Math.min(start + max, block.length());
            chunks.add(block.substring(start, end).trim());
            if (end == block.length()) {
                break;
            }
            start = end - overlapSize;
        }
    }

    private static void flush(List<String> chunks, StringBuilder current) {
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
            current.setLength(0);
        }
    }
}
