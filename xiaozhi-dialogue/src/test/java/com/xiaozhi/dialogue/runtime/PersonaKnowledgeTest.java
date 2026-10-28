package com.xiaozhi.dialogue.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 知识上下文插在系统提示词之后、历史之前：位置错了模型会把资料当成用户说过的话。
 */
class PersonaKnowledgeTest {

    @Test
    void knowledgeGoesRightAfterTheRolePrompt() {
        List<Message> messages = List.of(
            new SystemMessage("角色设定"),
            new UserMessage("你好"));

        List<Message> result = Persona.insertKnowledgeMessage(messages, "资料1：七天无理由");

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(((SystemMessage) result.get(0)).getText()).isEqualTo("角色设定");
        assertThat(((SystemMessage) result.get(1)).getText()).contains("七天无理由");
        assertThat(((UserMessage) result.get(2)).getText()).isEqualTo("你好");
    }

    @Test
    void emptyHistoryStillGetsTheKnowledgeMessage() {
        List<Message> result = Persona.insertKnowledgeMessage(List.of(), "资料1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(SystemMessage.class);
    }
}
