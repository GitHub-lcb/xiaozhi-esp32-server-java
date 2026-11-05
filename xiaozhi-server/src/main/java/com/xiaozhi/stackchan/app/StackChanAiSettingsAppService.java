package com.xiaozhi.stackchan.app;

import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.common.model.bo.ConversationBO;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.MemoryFactBO;
import com.xiaozhi.common.model.bo.MessageBO;
import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.common.model.bo.SummaryBO;
import com.xiaozhi.common.model.req.RoleUpdateReq;
import com.xiaozhi.config.service.ConfigService;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.memoryfact.service.MemoryFactService;
import com.xiaozhi.message.service.ConversationService;
import com.xiaozhi.message.service.MessageService;
import com.xiaozhi.role.RoleAppService;
import com.xiaozhi.role.service.RoleService;
import com.xiaozhi.server.web.chat.WebChatAppService;
import com.xiaozhi.stackchan.app.model.req.StackChanAiConversationRenameReq;
import com.xiaozhi.stackchan.app.model.req.StackChanAiRoleUpdateReq;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiConversationPageResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiConversationResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiFactResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiMemoryResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiMessagePageResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiMessageResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiModelOptionResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiOptionsResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiRoleResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiSttOptionResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiSummaryResp;
import com.xiaozhi.stackchan.app.model.resp.StackChanAiVoiceOptionResp;
import com.xiaozhi.summary.service.SummaryService;
import com.xiaozhi.utils.CommonUtils;
import com.xiaozhi.utils.DateUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * StackChan App「AI 设置」的编排服务：角色 / 模型音色 / 记忆 / 对话记录。
 * <p>
 * 契约见 {@code .artifacts/review/AI-SETTINGS-API-CONTRACT.md}，字段以它为准。
 * 这里只做 App 侧的归属校验、字段裁剪与结构适配，业务读写全部复用既有 Service：
 * {@link RoleService}/{@link RoleAppService}、{@link ConfigService}、{@link DeviceService}、
 * {@link SummaryService}、{@link MemoryFactService}、{@link ConversationService}、
 * {@link MessageService}、{@link WebChatAppService}。
 * <p>
 * 所有数据一律限定当前 userId：别人的角色/设备/会话/事实一律按「不存在」处理，
 * 且不向 App 下发 apiKey、baseUrl 等敏感字段。
 */
@Slf4j
@Service
public class StackChanAiSettingsAppService {

    private static final String CONFIG_TYPE_LLM = "llm";

    private static final String CONFIG_TYPE_TTS = "tts";

    private static final String CONFIG_TYPE_STT = "stt";

    private static final String MODEL_TYPE_CHAT = "chat";

    /** 契约要求记忆接口一次最多取 200 条（摘要 / 事实各自）。 */
    private static final int MEMORY_PAGE_SIZE = 200;

    private static final int MAX_PAGE_SIZE = 200;

    private static final int DEFAULT_CONVERSATION_PAGE_SIZE = 20;

    private static final int DEFAULT_MESSAGE_PAGE_SIZE = 50;

    private static final int ROLE_NAME_MAX_LENGTH = 20;

    private static final int CONVERSATION_TITLE_MAX_LENGTH = 100;

    @Resource
    private RoleService roleService;

    @Resource
    private RoleAppService roleAppService;

    @Resource
    private ConfigService configService;

    @Resource
    private DeviceService deviceService;

    @Resource
    private SummaryService summaryService;

    @Resource
    private MemoryFactService memoryFactService;

    @Resource
    private ConversationService conversationService;

    @Resource
    private MessageService messageService;

    @Resource
    private WebChatAppService webChatAppService;

    // ==================== 角色配置 ====================

    /**
     * 当前用户的默认 AI 角色配置（GET /role）。
     * <p>
     * 默认角色由 {@link RoleService#getDefaultOrFirstBO(Integer)} 取（isDefault=1，没有就第一个启用角色）；
     * 模型名 / 提供商 / TTS 提供商按角色上的配置 id 反查 {@code sys_config}，取不到就是 null。
     */
    public StackChanAiRoleResp getRole(Integer userId) {
        RoleBO role = requireDefaultRole(userId);

        StackChanAiRoleResp resp = new StackChanAiRoleResp();
        resp.setRoleId(role.getRoleId());
        resp.setRoleName(role.getRoleName());
        resp.setAvatar(role.getAvatar());
        resp.setVoiceName(role.getVoiceName());
        resp.setTtsPitch(role.getTtsPitch());
        resp.setTtsSpeed(role.getTtsSpeed());
        resp.setTemperature(role.getTemperature());
        resp.setTopP(role.getTopP());
        resp.setTtsId(role.getTtsId());
        resp.setModelId(role.getModelId());
        resp.setSttId(role.getSttId());
        resp.setVadSilenceMs(role.getVadSilenceMs());
        resp.setInactiveTimeoutSeconds(role.getInactiveTimeoutSeconds());
        resp.setUpdateTime(toEpochMilli(role.getUpdateTime()));

        ConfigBO model = configService.getBO(role.getModelId());
        if (model != null) {
            resp.setModelName(model.getConfigName());
            resp.setModelProvider(model.getProvider());
        }
        ConfigBO tts = configService.getBO(role.getTtsId());
        if (tts != null) {
            resp.setTtsProvider(tts.getProvider());
        }

        resp.setDeviceMac(findFirstDeviceMac(userId, role.getRoleId()));
        return resp;
    }

    /**
     * 只更新传入字段（PUT /role）。
     * <p>
     * 角色固定取当前用户的默认角色，请求体里没有 roleId，从协议上避免越权改别人的角色。
     * 局部更新语义由 {@link RoleAppService#update(Integer, RoleUpdateReq)} 与角色聚合根的
     * {@code merge} 保证：没传的字段保留原值。
     */
    public Boolean updateRole(Integer userId, StackChanAiRoleUpdateReq req) {
        if (req == null) {
            return Boolean.TRUE;
        }
        RoleBO role = requireDefaultRole(userId);

        RoleUpdateReq update = new RoleUpdateReq();
        boolean changed = false;

        if (req.getRoleName() != null) {
            String roleName = req.getRoleName().trim();
            if (roleName.isEmpty()) {
                throw StackChanAppException.business("Role name cannot be empty.");
            }
            if (roleName.codePointCount(0, roleName.length()) > ROLE_NAME_MAX_LENGTH) {
                throw StackChanAppException.business(
                    "Role name cannot exceed " + ROLE_NAME_MAX_LENGTH + " characters.");
            }
            update.setRoleName(roleName);
            changed = true;
        }
        if (req.getVoiceName() != null) {
            update.setVoiceName(req.getVoiceName().trim());
            changed = true;
        }
        if (req.getTtsPitch() != null) {
            checkRange(req.getTtsPitch(), 0.5d, 2.0d, "TTS pitch must be between 0.5 and 2.");
            update.setTtsPitch(req.getTtsPitch());
            changed = true;
        }
        if (req.getTtsSpeed() != null) {
            checkRange(req.getTtsSpeed(), 0.5d, 2.0d, "TTS speed must be between 0.5 and 2.");
            update.setTtsSpeed(req.getTtsSpeed());
            changed = true;
        }
        if (req.getTemperature() != null) {
            checkRange(req.getTemperature(), 0.0d, 1.0d, "Temperature must be between 0 and 1.");
            update.setTemperature(req.getTemperature());
            changed = true;
        }
        if (req.getTopP() != null) {
            checkRange(req.getTopP(), 0.0d, 1.0d, "Top-P must be between 0 and 1.");
            update.setTopP(req.getTopP());
            changed = true;
        }
        if (req.getVadSilenceMs() != null) {
            checkIntRange(req.getVadSilenceMs(), 100, 3000,
                "VAD silence time must be between 100 and 3000 milliseconds.");
            update.setVadSilenceMs(req.getVadSilenceMs());
            changed = true;
        }
        if (req.getInactiveTimeoutSeconds() != null) {
            checkIntRange(req.getInactiveTimeoutSeconds(), 10, 600,
                "Inactive timeout must be between 10 and 600 seconds.");
            update.setInactiveTimeoutSeconds(req.getInactiveTimeoutSeconds());
            changed = true;
        }
        if (req.getModelId() != null) {
            // 模型必须是自己名下的 llm 配置，避免把自己的角色指向别人的 apiKey。
            // 注册时复制的默认角色会继承管理员模板上的配置 id，原值回传视为「没改」，不做归属校验；
            // 只有换成新配置时才要求该配置确实归当前用户（选项接口下发的也只会是自己的配置）。
            if (!req.getModelId().equals(role.getModelId())) {
                requireOwnedConfig(userId, req.getModelId(), CONFIG_TYPE_LLM, "Model configuration not found.");
            }
            update.setModelId(req.getModelId());
            changed = true;
        }
        if (req.getTtsId() != null) {
            // 非正数是「服务端本地音色」的约定值，不指向 sys_config，不做归属校验
            if (req.getTtsId() > 0 && !req.getTtsId().equals(role.getTtsId())) {
                requireOwnedConfig(userId, req.getTtsId(), CONFIG_TYPE_TTS, "TTS configuration not found.");
            }
            update.setTtsId(req.getTtsId());
            changed = true;
        }
        if (req.getSttId() != null) {
            // 同上：非正数是「服务端本地识别」
            if (req.getSttId() > 0 && !req.getSttId().equals(role.getSttId())) {
                requireOwnedConfig(userId, req.getSttId(), CONFIG_TYPE_STT, "STT configuration not found.");
            }
            update.setSttId(req.getSttId());
            changed = true;
        }

        if (!changed) {
            return Boolean.TRUE;
        }
        roleAppService.update(role.getRoleId(), update);
        log.info("StackChan App 保存 AI 角色配置, userId={}, roleId={}", userId, role.getRoleId());
        return Boolean.TRUE;
    }

    // ==================== 可选模型 / 音色 / 识别引擎 ====================

    /**
     * 当前用户可选的模型 / 音色 / 识别引擎（GET /options）。
     * <p>
     * 只读 {@code sys_config} 里自己的、启用中的配置，出参只有 id / 名称 / 提供商 / 展示名，
     * 不含 apiKey、baseUrl 等任何敏感字段。没有可选项时返回空数组。
     */
    public StackChanAiOptionsResp options(Integer userId) {
        StackChanAiOptionsResp resp = new StackChanAiOptionsResp();

        List<StackChanAiModelOptionResp> models = new ArrayList<>();
        for (ConfigBO config : configService.listBO(userId, CONFIG_TYPE_LLM, null, null, null,
            ConfigBO.STATE_ENABLED)) {
            // 只有对话模型能绑到角色上；vision / intent / embedding 等配置不进下拉
            if (StringUtils.hasText(config.getModelType()) && !MODEL_TYPE_CHAT.equals(config.getModelType())) {
                continue;
            }
            StackChanAiModelOptionResp option = new StackChanAiModelOptionResp();
            option.setModelId(config.getConfigId());
            option.setName(config.getConfigName());
            option.setProvider(config.getProvider());
            option.setLabel(displayLabel(config));
            models.add(option);
        }
        resp.setModels(models);

        List<StackChanAiVoiceOptionResp> voices = new ArrayList<>();
        for (ConfigBO config : configService.listBO(userId, CONFIG_TYPE_TTS, null, null, null,
            ConfigBO.STATE_ENABLED)) {
            StackChanAiVoiceOptionResp option = new StackChanAiVoiceOptionResp();
            option.setTtsId(config.getConfigId());
            option.setName(config.getConfigName());
            option.setProvider(config.getProvider());
            option.setLabel(displayLabel(config));
            voices.add(option);
        }
        resp.setVoices(voices);

        List<StackChanAiSttOptionResp> stts = new ArrayList<>();
        for (ConfigBO config : configService.listBO(userId, CONFIG_TYPE_STT, null, null, null,
            ConfigBO.STATE_ENABLED)) {
            StackChanAiSttOptionResp option = new StackChanAiSttOptionResp();
            option.setSttId(config.getConfigId());
            option.setName(config.getConfigName());
            option.setLabel(displayLabel(config));
            stts.add(option);
        }
        resp.setStts(stts);

        return resp;
    }

    // ==================== 记忆 ====================

    /**
     * 某台设备的记忆（GET /memory）：摘要按 (deviceId, roleId) 查，事实按 userId 全量查。
     * <p>
     * deviceMac 不传时取当前用户第一台真实设备（跳过网页聊天虚拟设备）；
     * 传了就必须是自己名下的设备，否则按「设备不存在」报错。
     * 事实表没有设备维度（契约允许的降级），因此传任何设备看到的 facts 都是当前用户的全部事实。
     */
    public StackChanAiMemoryResp memory(Integer userId, String deviceMac) {
        DeviceBO device = resolveDevice(userId, deviceMac);

        StackChanAiMemoryResp resp = new StackChanAiMemoryResp();
        if (device == null) {
            resp.setFacts(listFacts(userId));
            return resp;
        }

        resp.setDeviceMac(toPureMac(device.getDeviceId()));
        resp.setRoleId(device.getRoleId());
        PageResult<SummaryBO> summaries = summaryService.page(
            device.getDeviceId(), userId, device.getRoleId(), 1, MEMORY_PAGE_SIZE);
        List<StackChanAiSummaryResp> summaryRows = new ArrayList<>();
        for (SummaryBO summary : summaries.getList()) {
            summaryRows.add(toSummaryResp(summary));
        }
        resp.setSummaries(summaryRows);
        resp.setFacts(listFacts(userId));
        return resp;
    }

    /**
     * 清空某台设备的记忆（DELETE /memory），返回实际删除的条数。
     * <p>
     * 只清摘要：事实表 {@code sys_memory_fact} 没有设备维度，按契约的降级规则不在这里删，
     * 单条事实走 DELETE /memory-fact/{factId}。
     */
    public Integer clearMemory(Integer userId, String deviceMac) {
        DeviceBO device = resolveDevice(userId, deviceMac);
        if (device == null) {
            return 0;
        }
        int deleted = device.getRoleId() == null
            ? summaryService.deleteByDeviceId(device.getDeviceId())
            : summaryService.delete(device.getRoleId(), device.getDeviceId(), null);
        log.info("StackChan App 清空设备记忆, userId={}, deviceId={}, roleId={}, deleted={}",
            userId, device.getDeviceId(), device.getRoleId(), deleted);
        return deleted;
    }

    /** 删除单条长期记忆事实（DELETE /memory-fact/{factId}），归属不是当前用户的按「不存在」处理。 */
    public Boolean deleteMemoryFact(Integer userId, Long factId) {
        if (factId == null) {
            throw StackChanAppException.business("Memory fact not found.");
        }
        try {
            memoryFactService.delete(factId, userId);
        } catch (ResourceNotFoundException e) {
            throw StackChanAppException.business("Memory fact not found.");
        }
        log.info("StackChan App 删除记忆事实, userId={}, factId={}", userId, factId);
        return Boolean.TRUE;
    }

    // ==================== 对话记录 ====================

    /** 当前用户的 Web 会话列表（GET /conversations），按最后对话时间倒序，带消息条数。 */
    public StackChanAiConversationPageResp conversations(Integer userId, Integer pageNo, Integer pageSize) {
        int no = normalizePageNo(pageNo);
        int size = normalizePageSize(pageSize, DEFAULT_CONVERSATION_PAGE_SIZE);

        PageResult<ConversationBO> page = conversationService.page(userId, null, no, size);
        List<ConversationBO> conversations = page.getList();

        List<String> sessionIds = new ArrayList<>(conversations.size());
        for (ConversationBO conversation : conversations) {
            sessionIds.add(conversation.getSessionId());
        }
        Map<String, Long> messageCounts = messageService.countBySessionIds(sessionIds);

        StackChanAiConversationPageResp resp = new StackChanAiConversationPageResp();
        resp.setTotal(page.getTotal());
        List<StackChanAiConversationResp> rows = new ArrayList<>(conversations.size());
        for (ConversationBO conversation : conversations) {
            rows.add(toConversationResp(conversation, messageCounts));
        }
        resp.setRows(rows);
        return resp;
    }

    /** 某个会话的消息列表（GET /conversations/{sessionId}/messages），按时间升序。 */
    public StackChanAiMessagePageResp messages(Integer userId, String sessionId,
                                              Integer pageNo, Integer pageSize) {
        ConversationBO conversation = requireOwnedConversation(userId, sessionId);

        int no = normalizePageNo(pageNo);
        int size = normalizePageSize(pageSize, DEFAULT_MESSAGE_PAGE_SIZE);
        PageResult<MessageBO> page = messageService.pageBySession(
            conversation.getSessionId(), userId, no, size);

        StackChanAiMessagePageResp resp = new StackChanAiMessagePageResp();
        resp.setTotal(page.getTotal());
        List<StackChanAiMessageResp> rows = new ArrayList<>(page.getList().size());
        for (MessageBO message : page.getList()) {
            rows.add(toMessageResp(message));
        }
        resp.setRows(rows);
        return resp;
    }

    /** 重命名会话（PATCH /conversations/{sessionId}），只允许改当前用户名下的会话。 */
    public Boolean renameConversation(Integer userId, String sessionId,
                                      StackChanAiConversationRenameReq req) {
        if (req == null || !StringUtils.hasText(req.getTitle())) {
            throw StackChanAppException.business("Title cannot be empty.");
        }
        String title = req.getTitle().trim();
        if (title.codePointCount(0, title.length()) > CONVERSATION_TITLE_MAX_LENGTH) {
            throw StackChanAppException.business(
                "Title cannot exceed " + CONVERSATION_TITLE_MAX_LENGTH + " characters.");
        }
        if (!StringUtils.hasText(sessionId)) {
            throw StackChanAppException.business("Conversation not found.");
        }
        try {
            conversationService.rename(userId, sessionId, title);
        } catch (ResourceNotFoundException e) {
            throw StackChanAppException.business("Conversation not found.");
        }
        log.info("StackChan App 重命名会话, userId={}, sessionId={}", userId, sessionId);
        return Boolean.TRUE;
    }

    /** 删除会话及其消息 / 摘要（DELETE /conversations/{sessionId}），不归属当前用户的按「不存在」处理。 */
    public Boolean deleteConversation(Integer userId, String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw StackChanAppException.business("Conversation not found.");
        }
        int deleted = webChatAppService.deleteConversations(userId, List.of(sessionId));
        if (deleted == 0) {
            throw StackChanAppException.business("Conversation not found.");
        }
        log.info("StackChan App 删除会话, userId={}, sessionId={}", userId, sessionId);
        return Boolean.TRUE;
    }

    // ==================== 内部工具 ====================

    private RoleBO requireDefaultRole(Integer userId) {
        RoleBO role = roleService.getDefaultOrFirstBO(userId);
        if (role == null || role.getRoleId() == null) {
            throw StackChanAppException.internal(
                "No AI role is configured for this account. Please create a role in the admin console first.");
        }
        return role;
    }

    private ConversationBO requireOwnedConversation(Integer userId, String sessionId) {
        ConversationBO conversation = StringUtils.hasText(sessionId)
            ? conversationService.get(sessionId)
            : null;
        if (conversation == null || !userId.equals(conversation.getUserId())) {
            throw StackChanAppException.business("Conversation not found.");
        }
        return conversation;
    }

    /**
     * 解析要操作的设备：传了 deviceMac 就必须是当前用户自己的设备；没传就取第一台真实设备。
     * 没有可用设备时返回 null（GET 返回空记忆，DELETE 返回 0）。
     */
    private DeviceBO resolveDevice(Integer userId, String deviceMac) {
        if (StringUtils.hasText(deviceMac)) {
            String normalized = requireMac(deviceMac);
            DeviceBO device = deviceService.getBO(normalized);
            if (device == null || !userId.equals(device.getUserId())) {
                throw StackChanAppException.business(
                    "Device not found or not bound to the current account.");
            }
            return device;
        }
        for (DeviceBO device : deviceService.listByUserId(userId)) {
            if (toPureMac(device.getDeviceId()) != null) {
                return device;
            }
        }
        return null;
    }

    /** 角色下第一台真实设备的 12 位纯 hex MAC；没有设备返回 null。 */
    private String findFirstDeviceMac(Integer userId, Integer roleId) {
        if (roleId == null) {
            return null;
        }
        for (DeviceBO device : deviceService.listByUserId(userId)) {
            if (!roleId.equals(device.getRoleId())) {
                continue;
            }
            String mac = toPureMac(device.getDeviceId());
            if (mac != null) {
                return mac;
            }
        }
        return null;
    }

    private List<StackChanAiFactResp> listFacts(Integer userId) {
        PageResult<MemoryFactBO> page = memoryFactService.page(1, MEMORY_PAGE_SIZE, null, null, userId);
        List<StackChanAiFactResp> facts = new ArrayList<>(page.getList().size());
        for (MemoryFactBO fact : page.getList()) {
            StackChanAiFactResp resp = new StackChanAiFactResp();
            resp.setFactId(fact.getFactId());
            resp.setContent(fact.getContent());
            resp.setCategory(fact.getCategory());
            resp.setSource(fact.getSource());
            resp.setCreateTime(toEpochMilli(fact.getCreateTime()));
            facts.add(resp);
        }
        return facts;
    }

    private StackChanAiSummaryResp toSummaryResp(SummaryBO summary) {
        StackChanAiSummaryResp resp = new StackChanAiSummaryResp();
        // 摘要表没有自增主键，与既有 SummaryResp 一致：用 createTime 毫秒时间戳当 id
        resp.setId(toEpochMilli(summary.getCreateTime()));
        resp.setSummary(summary.getSummary());
        resp.setCreateTime(toEpochMilli(summary.getCreateTime()));
        return resp;
    }

    private StackChanAiConversationResp toConversationResp(ConversationBO conversation,
                                                           Map<String, Long> messageCounts) {
        StackChanAiConversationResp resp = new StackChanAiConversationResp();
        resp.setSessionId(conversation.getSessionId());
        resp.setTitle(conversation.getTitle());
        resp.setMessageCount(messageCounts.getOrDefault(conversation.getSessionId(), 0L));
        // 会话表的 updateTime 就是最后一条消息的时间（ConversationService#touch 每轮刷新）
        resp.setLastMessageTime(toEpochMilli(conversation.getUpdateTime()));
        return resp;
    }

    private StackChanAiMessageResp toMessageResp(MessageBO message) {
        StackChanAiMessageResp resp = new StackChanAiMessageResp();
        resp.setMessageId(message.getMessageId());
        // 库里叫 sender，契约里叫 role
        resp.setRole(message.getSender());
        resp.setContent(message.getMessage());
        resp.setCreateTime(toEpochMilli(message.getCreateTime()));
        return resp;
    }

    private String displayLabel(ConfigBO config) {
        return StringUtils.hasText(config.getConfigDesc()) ? config.getConfigDesc() : config.getConfigName();
    }

    private ConfigBO requireOwnedConfig(Integer userId, Integer configId, String configType,
                                        String notFoundMessage) {
        ConfigBO config = configService.getBO(configId);
        if (config == null || !userId.equals(config.getUserId())
            || !configType.equals(config.getConfigType())) {
            throw StackChanAppException.business(notFoundMessage);
        }
        return config;
    }

    private static void checkRange(Double value, double min, double max, String message) {
        if (value == null) {
            return;
        }
        if (value.isNaN() || value < min || value > max) {
            throw StackChanAppException.business(message);
        }
    }

    private static void checkIntRange(Integer value, int min, int max, String message) {
        if (value == null) {
            return;
        }
        if (value < min || value > max) {
            throw StackChanAppException.business(message);
        }
    }

    private static int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private static int normalizePageSize(Integer pageSize, int defaultSize) {
        if (pageSize == null) {
            return defaultSize;
        }
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    /**
     * 把 App 传来的 MAC 归一化成内部 deviceId（小写冒号格式）。
     * 支持 12 位纯 hex、冒号分隔、短横线分隔，大小写不限；非 MAC（如 user_chat_1）返回 null。
     * 与 {@link StackChanAppDeviceAppService} 同一口径。
     */
    private String normalizeMac(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String compact = raw.trim().replace("-", "").replace(":", "");
        if (compact.length() != 12) {
            return null;
        }
        for (int i = 0; i < compact.length(); i++) {
            if (Character.digit(compact.charAt(i), 16) < 0) {
                return null;
            }
        }

        String lower = compact.toLowerCase(Locale.ROOT);
        StringBuilder mac = new StringBuilder(17);
        for (int i = 0; i < lower.length(); i += 2) {
            if (i > 0) {
                mac.append(':');
            }
            mac.append(lower, i, i + 2);
        }
        String normalized = mac.toString();
        // 与 OTA / 设备侧鉴权同一口径：只接受单播 MAC
        return CommonUtils.isMacAddressValid(normalized) ? normalized : null;
    }

    private String requireMac(String raw) {
        String mac = normalizeMac(raw);
        if (mac == null) {
            throw StackChanAppException.business("Invalid device MAC address.");
        }
        return mac;
    }

    /** 内部 deviceId（小写冒号 MAC）→ App 的 12 位小写纯 hex；非真实设备返回 null。 */
    private String toPureMac(String deviceId) {
        String normalized = normalizeMac(deviceId);
        return normalized == null ? null : normalized.replace(":", "");
    }

    private static Long toEpochMilli(LocalDateTime time) {
        return time == null ? null : DateUtils.toInstant(time).toEpochMilli();
    }

    private static Long toEpochMilli(Instant time) {
        return time == null ? null : time.toEpochMilli();
    }
}
