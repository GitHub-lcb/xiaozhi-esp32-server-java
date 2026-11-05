package com.xiaozhi.file;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验签路径还原的回归测试。
 * <p>
 * 背景：签名是对磁盘上的原始文件名算的，浏览器请求时会把空格/中文做百分号编码。
 * 曾经因为直接拿编码后的 URI 去验签，导致 {@code Kevin MacLeod - Carefree.mp3}、
 * {@code 测试.mp3} 这类文件试听/下载一律 403。
 */
class LocalFileAccessInterceptorTest {

    private static String requestPath(String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(requestUri);
        return LocalFileAccessInterceptor.requestPath(request);
    }

    @Test
    @DisplayName("纯 ASCII 路径原样返回")
    void keepsAsciiPathUnchanged() {
        assertThat(requestPath("/uploads/music/kevin-macleod-carefree.mp3"))
                .isEqualTo("uploads/music/kevin-macleod-carefree.mp3");
    }

    @Test
    @DisplayName("空格被编码成 %20 时还原为真实文件名")
    void decodesEncodedSpaces() {
        assertThat(requestPath("/uploads/music/Kevin%20MacLeod%20-%20Carefree.mp3"))
                .isEqualTo("uploads/music/Kevin MacLeod - Carefree.mp3");
    }

    @Test
    @DisplayName("中文文件名按 UTF-8 还原")
    void decodesChineseFileName() {
        String encoded = UriEncodeHelper.encode("uploads/music/测试 - 第一首.mp3");
        assertThat(requestPath("/" + encoded))
                .isEqualTo("uploads/music/测试 - 第一首.mp3");
    }

    @Test
    @DisplayName("+ 号保持原样，不会被当成空格")
    void keepsPlusSign() {
        assertThat(requestPath("/uploads/music/a+b.mp3"))
                .isEqualTo("uploads/music/a+b.mp3");
    }

    @Test
    @DisplayName("上下文路径会被去掉")
    void stripsContextPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/api");
        request.setRequestURI("/api/uploads/music/a.mp3");
        assertThat(LocalFileAccessInterceptor.requestPath(request))
                .isEqualTo("uploads/music/a.mp3");
    }

    @Test
    @DisplayName("非法百分号编码时回退原文，由验签流程拒绝")
    void fallsBackToRawPathWhenEncodingIsBroken() {
        assertThat(requestPath("/uploads/music/bad%zz.mp3"))
                .isEqualTo("uploads/music/bad%zz.mp3");
    }

    /** 仅用于构造百分号编码的测试数据，避免手写一长串转义 */
    private static final class UriEncodeHelper {
        private UriEncodeHelper() {
        }

        static String encode(String raw) {
            StringBuilder builder = new StringBuilder();
            for (byte b : raw.getBytes(StandardCharsets.UTF_8)) {
                int value = b & 0xFF;
                char ch = (char) value;
                if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')
                        || ch == '-' || ch == '_' || ch == '.' || ch == '~' || ch == '/') {
                    builder.append(ch);
                } else {
                    builder.append('%').append(String.format("%02X", value));
                }
            }
            return builder.toString();
        }
    }
}
