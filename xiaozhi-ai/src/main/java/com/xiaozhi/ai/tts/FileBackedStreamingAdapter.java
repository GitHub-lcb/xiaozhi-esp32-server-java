package com.xiaozhi.ai.tts;

import com.xiaozhi.utils.AudioUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;
import java.util.List;

/**
 * 把「只支持整句合成」的 TTS 包装成流式接口。
 * <p>
 * 它并不缩短首包：仍然要等整句合成完，只是把结果按与旧路径完全相同的分块切法吐出去，
 * 让上层只有一条代码路径（{@code synthesizeStream}），便于逐 provider 替换成真流式。
 * 之所以保留它：迁移期间要能用同一套测试断言「帧序列与改造前逐字节一致」。
 */
public class FileBackedStreamingAdapter implements StreamingTtsService {

    private final TtsService delegate;

    public FileBackedStreamingAdapter(TtsService delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate 不能为空");
        }
        this.delegate = delegate;
    }

    @Override
    public String getProviderName() {
        return delegate.getProviderName();
    }

    @Override
    public XiaozhiTtsOptions getOptions() {
        return delegate.getOptions();
    }

    @Override
    public String audioFormat() {
        return delegate.audioFormat();
    }

    @Override
    public Path textToSpeech(String text) throws Exception {
        return delegate.textToSpeech(text);
    }

    @Override
    public Flux<byte[]> synthesizeStream(String text) {
        return Flux.defer(() -> {
            try {
                Path audioPath = delegate.textToSpeech(text);
                if (audioPath == null) {
                    return Flux.empty();
                }
                // 与 FileSynthesizer 旧路径使用同一个分块读取方法，保证帧序列一致
                List<byte[]> chunks = AudioUtils.readAsPcmChunks(audioPath.toString());
                return Flux.fromIterable(chunks);
            } catch (Exception e) {
                return Flux.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public boolean nativeStreaming() {
        return false;
    }

    /** 供上层判断是否需要包装 */
    public static StreamingTtsService wrapIfNeeded(TtsService service) {
        if (service instanceof StreamingTtsService streaming) {
            return streaming;
        }
        return new FileBackedStreamingAdapter(service);
    }
}
