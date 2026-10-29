package com.xiaozhi.ai.tts.providers;

import com.xiaozhi.ai.tts.providers.VolcengineTtsService.ParsedChunk;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分块解析是流式合成的要害：解析错了要么丢音频（听不全），要么把结束码当错误（每句都中断）。
 */
class VolcengineChunkParsingTest {

    @Test
    void blankAndGarbageLinesAreSkipped() {
        assertThat(VolcengineTtsService.parseChunk(null).audio()).isNull();
        assertThat(VolcengineTtsService.parseChunk("   ").audio()).isNull();
        assertThat(VolcengineTtsService.parseChunk("not-json").error()).isNull();
    }

    @Test
    void successCodeIsNotAnError() {
        ParsedChunk chunk = VolcengineTtsService.parseChunk("{\"code\":20000000,\"message\":\"OK\"}");

        assertThat(chunk.error()).isNull();
        assertThat(chunk.audio()).isNull();
    }

    @Test
    void nonZeroCodeBecomesError() {
        ParsedChunk chunk = VolcengineTtsService.parseChunk("{\"code\":55000000,\"message\":\"resource mismatch\"}");

        assertThat(chunk.error()).contains("55000000").contains("resource mismatch");
        assertThat(chunk.audio()).isNull();
    }

    @Test
    void base64DataIsDecoded() {
        byte[] pcm = new byte[]{1, 2, 3, 4};
        String line = "{\"code\":0,\"data\":\"" + Base64.getEncoder().encodeToString(pcm) + "\"}";

        assertThat(VolcengineTtsService.parseChunk(line).audio()).isEqualTo(pcm);
    }

    @Test
    void emptyDataYieldsNothing() {
        assertThat(VolcengineTtsService.parseChunk("{\"data\":\"\"}").audio()).isNull();
        assertThat(VolcengineTtsService.parseChunk("{\"data\":null}").audio()).isNull();
    }
}
