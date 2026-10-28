package com.xiaozhi.ai.memory;

import com.xiaozhi.common.model.bo.MemoryFactBO;
import com.xiaozhi.memoryfact.service.MemoryFactService;
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
import static org.mockito.Mockito.when;

/**
 * 记忆注入：按"说话人 + 全家"取，没记忆或加载失败返回 null（不能给模型一段空壳提示）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemoryFactProviderTest {

    @Mock
    private MemoryFactService memoryFactService;

    @Mock
    private com.xiaozhi.memorygraph.service.MemoryGraphService memoryGraphService;

    @InjectMocks
    private MemoryFactProvider provider;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(provider, "limit", 8);
        when(memoryGraphService.listRelationStatements(any(), anyInt())).thenReturn(java.util.List.of());
    }

    @Test
    void formatsFactsWithOwner() {
        when(memoryFactService.listForPrompt(any(), any(), anyInt())).thenReturn(List.of(
            fact("妈妈", "喜欢喝茶"), fact(null, "家里有只猫叫咪咪")));

        String context = provider.buildContext(1, "妈妈");

        assertThat(context).contains("妈妈：喜欢喝茶").contains("家里有只猫叫咪咪");
        assertThat(context).contains("以用户当前说法为准");
    }

    @Test
    void relationsAreAppendedWhenPresent() {
        when(memoryFactService.listForPrompt(any(), any(), anyInt())).thenReturn(List.of());
        when(memoryGraphService.listRelationStatements(any(), anyInt())).thenReturn(List.of("妈妈 喜欢 乌龙茶"));

        String context = provider.buildContext(1, "妈妈");

        assertThat(context).contains("人物与物品之间的关系").contains("妈妈 喜欢 乌龙茶");
    }

    @Test
    void returnsNullWhenNoFact() {
        when(memoryFactService.listForPrompt(any(), any(), anyInt())).thenReturn(List.of());

        assertThat(provider.buildContext(1, null)).isNull();
    }

    @Test
    void returnsNullInsteadOfThrowingOnFailure() {
        when(memoryFactService.listForPrompt(any(), any(), anyInt())).thenThrow(new IllegalStateException("db down"));

        assertThat(provider.buildContext(1, "妈妈")).isNull();
    }

    private MemoryFactBO fact(String member, String content) {
        MemoryFactBO bo = new MemoryFactBO();
        bo.setMemberName(member);
        bo.setContent(content);
        return bo;
    }
}
