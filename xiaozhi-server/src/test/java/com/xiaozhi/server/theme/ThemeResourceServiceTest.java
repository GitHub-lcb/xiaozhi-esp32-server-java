package com.xiaozhi.server.theme;

import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.role.service.RoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** 主题资源是设备端契约：字段缺失会让固件解析失败，所以形状要钉住。 */
@ExtendWith(MockitoExtension.class)
class ThemeResourceServiceTest {

    @Mock
    private RoleService roleService;

    @InjectMocks
    private ThemeResourceService service;

    @Test
    void buildsContractWithWakeWordsAndTheme() {
        RoleBO role = new RoleBO();
        role.setRoleId(3);
        role.setRoleName("小智");
        role.setRoleDesc("小智，小宝");
        when(roleService.getBO(3)).thenReturn(role);

        Map<String, Object> resource = service.build(3);

        assertThat(resource).containsKeys("version", "roleId", "wakeWords", "emojis", "theme");
        List<?> wakeWords = (List<?>) resource.get("wakeWords");
        assertThat(wakeWords).hasSize(2);
        assertThat(wakeWords.get(0)).isEqualTo("小智");
        assertThat(wakeWords.get(1)).isEqualTo("小宝");
    }

    @Test
    void unknownRoleFailsLoudly() {
        when(roleService.getBO(99)).thenReturn(null);

        assertThatThrownBy(() -> service.build(99)).hasMessageContaining("角色不存在");
    }

    @Test
    void wakeWordsAreDedupedAndCapped() {
        RoleBO role = new RoleBO();
        role.setRoleName("小智");
        role.setRoleDesc("小智,小智,小宝,阿宝,小美,小白,小蓝,小绿,小紫,小粉");

        List<String> words = ThemeResourceService.wakeWords(role);

        assertThat(words).hasSize(8);
        assertThat(words.get(0)).isEqualTo("小智");
        assertThat(words).doesNotHaveDuplicates();
    }
}