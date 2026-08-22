package com.yiliao.ai.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * 指南章节切分（specs/modules/ai.md §4.4）：按 markdown 标题（#/##）优先切，
 * 无标题结构按 chunkSize 字符滑窗（重叠 overlap），元数据保留文档名+章节。
 */
public final class TextChunker {

    private final int chunkSize;
    private final int overlap;

    public TextChunker(int chunkSize, int overlap) {
        if (chunkSize <= overlap) {
            throw new IllegalArgumentException("chunkSize 必须大于 overlap");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public record Chunk(String docName, String section, String text) {
    }

    public List<Chunk> chunk(String docName, String content) {
        List<Chunk> chunks = new ArrayList<>();
        String[] lines = content.split("\n");
        String section = "正文";
        StringBuilder buffer = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                flush(buffer, docName, section, chunks);
                section = trimmed.replaceAll("^#+\\s*", "");
                continue;
            }
            buffer.append(line).append('\n');
            if (buffer.length() >= chunkSize) {
                flush(buffer, docName, section, chunks);
            }
        }
        flush(buffer, docName, section, chunks);
        return chunks;
    }

    /** 滑窗：长段落切出重叠片段（保留章节元数据）。 */
    private void flush(StringBuilder buffer, String docName, String section, List<Chunk> chunks) {
        String text = buffer.toString().trim();
        buffer.setLength(0);
        if (text.isEmpty()) {
            return;
        }
        if (text.length() <= chunkSize) {
            chunks.add(new Chunk(docName, section, text));
            return;
        }
        int step = chunkSize - overlap;
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(text.length(), start + chunkSize);
            chunks.add(new Chunk(docName, section, text.substring(start, end)));
            if (end == text.length()) {
                break;
            }
        }
    }
}
