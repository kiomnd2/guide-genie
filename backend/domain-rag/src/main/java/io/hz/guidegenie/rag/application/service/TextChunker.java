package io.hz.guidegenie.rag.application.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 단순 토큰 근사(단어 기준) 청크 분할기. 기획 기준 500~1,000 토큰, 오버랩 100.
 * 실제 운영에서는 tokenizer 기반으로 교체 가능.
 */
public class TextChunker {

    private final int chunkSize;
    private final int overlap;

    public TextChunker(int chunkSize, int overlap) {
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String[] words = text.split("\\s+");
        int step = Math.max(1, chunkSize - overlap);
        for (int start = 0; start < words.length; start += step) {
            int end = Math.min(words.length, start + chunkSize);
            chunks.add(String.join(" ", List.of(words).subList(start, end)));
            if (end == words.length) {
                break;
            }
        }
        return chunks;
    }
}
