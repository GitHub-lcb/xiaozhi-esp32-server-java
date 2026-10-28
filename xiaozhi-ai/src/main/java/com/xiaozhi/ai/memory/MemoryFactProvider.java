package com.xiaozhi.ai.memory;

import com.xiaozhi.common.model.bo.MemoryFactBO;
import com.xiaozhi.memoryfact.service.MemoryFactService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 把长期记忆拼进本轮提示词。
 * <p>
 * 按"说话人 + 全家"两层取：声纹认出了妈妈就取妈妈的记忆，没认出来只取全家共用的，
 * 避免把别人的偏好套到当前说话人身上。
 */
@Slf4j
@Component
public class MemoryFactProvider {

    @Value("${xiaozhi.memory.fact-limit:8}")
    private int limit = 8;

    @Resource
    private MemoryFactService memoryFactService;

    @Resource
    private com.xiaozhi.memorygraph.service.MemoryGraphService memoryGraphService;

    public String buildContext(Integer userId, String memberName) {
        if (userId == null) {
            return null;
        }
        try {
            List<MemoryFactBO> facts = memoryFactService.listForPrompt(userId, memberName, limit);
            List<String> relations = memoryGraphService.listRelationStatements(userId, limit);
            if (facts.isEmpty() && relations.isEmpty()) {
                return null;
            }
            StringBuilder builder = new StringBuilder("关于当前用户你记得这些事：").append(System.lineSeparator());
            for (MemoryFactBO fact : facts) {
                String owner = StringUtils.hasText(fact.getMemberName()) ? fact.getMemberName() + "：" : "";
                builder.append("- ").append(owner).append(fact.getContent()).append(System.lineSeparator());
            }
            if (!relations.isEmpty()) {
                builder.append("人物与物品之间的关系：").append(System.lineSeparator());
                for (String relation : relations) {
                    builder.append("- ").append(relation).append(System.lineSeparator());
                }
            }
            builder.append("只在相关时自然使用；用户说到与记忆冲突的内容时，以用户当前说法为准。");
            return builder.toString().strip();
        } catch (RuntimeException e) {
            log.warn("长期记忆加载失败，本轮按无记忆处理 userId={}", userId, e);
            return null;
        }
    }
}
