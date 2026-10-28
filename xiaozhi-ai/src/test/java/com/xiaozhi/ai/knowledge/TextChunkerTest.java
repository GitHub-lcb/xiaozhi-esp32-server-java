package com.xiaozhi.ai.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 切片是检索质量的地基：切太碎丢上下文，切太大召回后塞不进提示词。
 */
class TextChunkerTest {

    @Test
    void blankTextProducesNoChunks() {
        assertThat(TextChunker.chunk(null)).isEmpty();
        assertThat(TextChunker.chunk("   \n\n  ")).isEmpty();
    }

    @Test
    void shortParagraphsAreMergedUpToTheLimit() {
        String text = "第一段内容。\n\n第二段内容。\n\n第三段内容。";

        List<String> chunks = TextChunker.chunk(text, 50, 10);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("第一段").contains("第三段");
    }

    @Test
    void longTextIsSplitIntoSeveralChunks() {
        String paragraph = "这是一段很长的内容，".repeat(60);

        List<String> chunks = TextChunker.chunk(paragraph, 200, 20);

        assertThat(chunks).hasSizeGreaterThan(1);
        chunks.forEach(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(200));
    }

    @Test
    void chunksKeepAnOverlapSoSentencesAreNotCutInHalf() {
        String paragraph = "0123456789".repeat(30);

        List<String> chunks = TextChunker.chunk(paragraph, 100, 20);

        assertThat(chunks).hasSizeGreaterThan(1);
        String firstTail = chunks.get(0).substring(chunks.get(0).length() - 20);
        assertThat(chunks.get(1)).startsWith(firstTail);
    }

    @Test
    void paragraphsLongerThanTheLimitAreFlushedSeparately() {
        String text = "短段落。\n\n" + "长".repeat(600) + "\n\n结尾段落。";

        List<String> chunks = TextChunker.chunk(text, 200, 20);

        assertThat(chunks.size()).isGreaterThanOrEqualTo(3);
        assertThat(chunks.get(chunks.size() - 1)).contains("结尾段落");
    }
}
