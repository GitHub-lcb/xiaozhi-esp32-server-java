package com.xiaozhi.server.theme;

import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.role.service.RoleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备主题资源生成：把角色的唤醒词、称呼、表情与配色整理成设备可直接读取的 JSON。
 * <p>
 * 只输出配置，不产出二进制资源（字体/图片需要另一条资源管线）；
 * 这样设备端拿到的是一份稳定契约，替换素材不用改协议。
 */
@Slf4j
@Service
public class ThemeResourceService {

    private static final List<String> DEFAULT_EMOJIS = List.of("😊", "🤔", "😄", "😴", "🎉");

    @Resource
    private RoleService roleService;

    public Map<String, Object> build(Integer roleId) {
        RoleBO role = roleId == null ? null : roleService.getBO(roleId);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在：" + roleId);
        }
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("version", 1);
        resource.put("roleId", role.getRoleId());
        resource.put("roleName", role.getRoleName());
        resource.put("wakeWords", wakeWords(role));
        resource.put("emojis", DEFAULT_EMOJIS);
        resource.put("theme", theme(role));
        return resource;
    }

    /**
     * 唤醒词：角色名本身总是可用；角色里额外配了唤醒词就并进去，去重保序。
     */
    static List<String> wakeWords(RoleBO role) {
        List<String> words = new ArrayList<>();
        if (role != null && StringUtils.hasText(role.getRoleName())) {
            words.add(role.getRoleName().trim());
        }
        if (role != null && StringUtils.hasText(role.getRoleDesc())) {
            for (String candidate : role.getRoleDesc().split("[,，、\\s]+")) {
                String trimmed = candidate.trim();
                if (!trimmed.isEmpty() && !words.contains(trimmed) && trimmed.length() <= 8) {
                    words.add(trimmed);
                }
            }
        }
        return words.size() > 8 ? words.subList(0, 8) : words;
    }

    static Map<String, Object> theme(RoleBO role) {
        Map<String, Object> theme = new LinkedHashMap<>();
        theme.put("primaryColor", "#1677ff");
        theme.put("background", "#0b1220");
        theme.put("fontScale", 1.0);
        if (role != null && StringUtils.hasText(role.getRoleName())) {
            theme.put("avatarText", role.getRoleName().substring(0, 1));
        }
        return theme;
    }
}