package com.xiaozhi.ai.knowledge;

import com.xiaozhi.common.model.bo.KnowledgeBaseBO;
import com.xiaozhi.common.model.bo.KnowledgeChunkBO;
import com.xiaozhi.knowledge.service.KnowledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * 知识注入的两个底线：没有资料时返回 null（不能给模型一段空壳提示），
 * 检索失败时也不能把整轮对话带崩。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KnowledgeContextProviderTest {

    @Mock
    private KnowledgeService knowledgeService;

    @Mock
    private KnowledgeRetriever knowledgeRetriever;

    @InjectMocks
    private KnowledgeContextProvider provider;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(provider, "topK", 4);
        ReflectionTestUtils.setField(provider, "maxChunkChars", 20);
    }

    @Test
    void formatsHitsWithUsageGuidance() {
        when(knowledgeService.listByRole(7)).thenReturn(List.of(base()));
        when(knowledgeRetriever.search(anyList(), any(), anyInt())).thenReturn(List.of(chunk("退货政策是七天无理由"), chunk("运费由买家承担")));

        String context = provider.buildContext(7, "怎么退货");

        assertThat(context).contains("资料1").contains("资料2").contains("七天无理由");
        assertThat(context).contains("不要编造");
    }

    @Test
    void returnsNullWhenRoleHasNoKnowledgeBase() {
        when(knowledgeService.listByRole(7)).thenReturn(List.of());

        assertThat(provider.buildContext(7, "怎么退货")).isNull();
    }

    @Test
    void returnsNullWhenNothingMatched() {
        when(knowledgeService.listByRole(7)).thenReturn(List.of(base()));
        when(knowledgeRetriever.search(anyList(), any(), anyInt())).thenReturn(List.of());

        assertThat(provider.buildContext(7, "怎么退货")).isNull();
    }

    @Test
    void returnsNullInsteadOfThrowingWhenRetrievalFails() {
        when(knowledgeService.listByRole(7)).thenReturn(List.of(base()));
        when(knowledgeRetriever.search(anyList(), any(), anyInt())).thenThrow(new IllegalStateException("向量服务不可用"));

        assertThat(provider.buildContext(7, "怎么退货")).isNull();
    }

    @Test
    void longChunkIsTruncated() {
        when(knowledgeService.listByRole(7)).thenReturn(List.of(base()));
        when(knowledgeRetriever.search(anyList(), any(), anyInt()))
            .thenReturn(List.of(chunk("很长的一段资料".repeat(10))));

        String context = provider.buildContext(7, "问题");

        assertThat(context).contains("...");
    }

    @Test
    void missingRoleOrQueryReturnsNull() {
        assertThat(provider.buildContext(null, "问题")).isNull();
        assertThat(provider.buildContext(7, "  ")).isNull();
    }

    private KnowledgeBaseBO base() {
        KnowledgeBaseBO bo = new KnowledgeBaseBO();
        bo.setKnowledgeId(1);
        bo.setName("客服手册");
        bo.setState(KnowledgeBaseBO.STATE_ENABLED);
        return bo;
    }

    private KnowledgeChunkBO chunk(String content) {
        KnowledgeChunkBO bo = new KnowledgeChunkBO();
        bo.setChunkId(1L);
        bo.setKnowledgeId(1);
        bo.setContent(content);
        return bo;
    }
}
