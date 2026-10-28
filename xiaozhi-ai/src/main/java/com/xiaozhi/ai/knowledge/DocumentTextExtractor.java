package com.xiaozhi.ai.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.ContentHandler;

import java.io.InputStream;

/**
 * 文档正文抽取：Tika 统一处理 PDF / Word / Excel / PPT / HTML / 纯文本，
 * 不按扩展名分支，避免「docx 改名成 doc 就解析不了」这类问题。
 */
@Slf4j
@Component
public class DocumentTextExtractor {

    /** 单份文档最多抽取的字符数，防止一份超大 PDF 把内存和嵌入成本打爆 */
    private static final int MAX_CHARS = 200_000;

    public String extract(InputStream inputStream) throws Exception {
        ContentHandler handler = new BodyContentHandler(MAX_CHARS);
        Metadata metadata = new Metadata();
        new AutoDetectParser().parse(inputStream, handler, metadata, new ParseContext());
        String text = handler.toString();
        log.debug("文档解析完成，字符数={}", text == null ? 0 : text.length());
        return text == null ? "" : text;
    }
}
