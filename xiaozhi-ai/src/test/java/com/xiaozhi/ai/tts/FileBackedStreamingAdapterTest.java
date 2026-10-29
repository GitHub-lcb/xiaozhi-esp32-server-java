package com.xiaozhi.ai.tts;

import com.xiaozhi.utils.AudioUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 伪流式适配器的契约：分块序列必须与旧路径逐字节一致（否则改造会悄悄改变下发音频），
 * 包装幂等（已经是流式的 provider 不能被再包一层）。
 */
class FileBackedStreamingAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void chunksAreIdenticalToLegacyPath() throws Exception {
        // 自己拼一个合法的 16k/单声道/16bit WAV，不依赖 AudioUtils 里需要 Spring 配置的落盘目录
        byte[] pcm = new byte[16_000 * 2 / 10]; // 100ms 静音
        Path wav = tempDir.resolve("sample.wav");
        Files.write(wav, wavBytes(pcm));

        TtsService delegate = new StubTtsService(wav);
        FileBackedStreamingAdapter adapter = new FileBackedStreamingAdapter(delegate);

        List<byte[]> legacy = AudioUtils.readAsPcmChunks(wav.toString());
        List<byte[]> streamed = adapter.synthesizeStream("你好").collectList().block();

        assertThat(streamed).isNotNull();
        assertThat(streamed).hasSameSizeAs(legacy);
        for (int i = 0; i < legacy.size(); i++) {
            assertThat(streamed.get(i)).isEqualTo(legacy.get(i));
        }
        assertThat(adapter.nativeStreaming()).isFalse();
    }

    @Test
    void wrappingIsIdempotentForNativeStreamingService() {
        TtsService nativeOne = new StreamingTtsService() {
            @Override
            public String getProviderName() {
                return "native";
            }

            @Override
            public XiaozhiTtsOptions getOptions() {
                return new XiaozhiTtsOptions("female", 1.0, 1.0);
            }

            @Override
            public Path textToSpeech(String text) {
                return null;
            }

            @Override
            public reactor.core.publisher.Flux<byte[]> synthesizeStream(String text) {
                return reactor.core.publisher.Flux.just(new byte[]{1});
            }
        };

        assertThat(FileBackedStreamingAdapter.wrapIfNeeded(nativeOne)).isSameAs(nativeOne);
    }

    @Test
    void nullAudioProducesEmptyStream() {
        FileBackedStreamingAdapter adapter = new FileBackedStreamingAdapter(new StubTtsService(null));

        assertThat(adapter.synthesizeStream("你好").collectList().block()).isEmpty();
    }

    /** 44 字节标准头 + PCM 数据 */
    private static byte[] wavBytes(byte[] pcm) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(44 + pcm.length).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        buffer.putInt(36 + pcm.length);
        buffer.put("WAVEfmt ".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        buffer.putInt(16);
        buffer.putShort((short) 1);   // PCM
        buffer.putShort((short) 1);   // 单声道
        buffer.putInt(16_000);        // 采样率
        buffer.putInt(16_000 * 2);    // 字节率
        buffer.putShort((short) 2);   // 块对齐
        buffer.putShort((short) 16);  // 位深
        buffer.put("data".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        buffer.putInt(pcm.length);
        buffer.put(pcm);
        return buffer.array();
    }

    /** 只实现必需方法的最小桩 */
    private static final class StubTtsService implements TtsService {
        private final Path path;

        private StubTtsService(Path path) {
            this.path = path;
        }

        @Override
        public String getProviderName() {
            return "stub";
        }

        @Override
        public XiaozhiTtsOptions getOptions() {
            return new XiaozhiTtsOptions("female", 1.0, 1.0);
        }

        @Override
        public Path textToSpeech(String text) {
            return path;
        }
    }
}
