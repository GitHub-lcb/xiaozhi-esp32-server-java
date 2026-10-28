package com.xiaozhi.ai.knowledge;

import com.xiaozhi.common.model.bo.KnowledgeBaseBO;
import com.xiaozhi.common.model.bo.KnowledgeChunkBO;
import com.xiaozhi.knowledge.service.KnowledgeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 对话前的知识检索：把角色挂载的知识库召回结果拼成一段系统提示词。
 * <p>
 * 召回失败不能让整轮对话失败——知识库是增强项，检索挂了就当作没有知识库继续回答，
 * 只记一条告警。
 */
@Slf4j
@Component
public class KnowledgeContextProvider {

    /** 单轮注入的最大分片数，太多会挤占上下文并稀释注意力 */
    @Value("${xiaozhi.knowledge.top-k:4}")
    private int topK = 4;

    /** 单条分片注入时的最大字符数，避免一条长分片吃掉预算 */
    @Value("${xiaozhi.knowledge.max-chunk-chars:400}")
    private int maxChunkChars = 400;

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private KnowledgeRetriever knowledgeRetriever;

    /**
     * @return 可直接作为 SystemMessage 的文本；没有可用知识时返回 null
     */
    public String buildContext(Integer roleId, String query) {
        if (roleId == null || query == null || query.isBlank()) {
            return null;
        }
        try {
            List<KnowledgeBaseBO> bases = knowledgeService.listByRole(roleId);
            if (bases.isEmpty()) {
                return null;
            }
            List<KnowledgeChunkBO> hits = knowledgeRetriever.search(bases, query, topK);
            if (hits.isEmpty()) {
                return null;
            }
            StringBuilder builder = new StringBuilder();
            builder.append("以下是该角色的知识库资料，供你回答时参考：")
                .append(System.lineSeparator())
                .append("1. 资料与用户问题无关时忽略，不要生搬硬套；")
                .append(System.lineSeparator())
                .append("2. 资料没有覆盖的内容，不要编造，按你原本的能力回答；")
                .append(System.lineSeparator());
            int index = 1;
            for (KnowledgeChunkBO hit : hits) {
                String content = hit.getContent();
                if (content == null || content.isBlank()) {
                    continue;
                }
                if (content.length() > maxChunkChars) {
                    content = content.substring(0, maxChunkChars) + "...";
                }
                builder.append("资料").append(index++).append("：").append(content.trim())
                    .append(System.lineSeparator());
            }
            return index == 1 ? null : builder.toString().strip();
        } catch (RuntimeException e) {
            log.warn("知识库检索失败，本轮按无知识库处理 roleId={}", roleId, e);
            return null;
        }
    }
}
