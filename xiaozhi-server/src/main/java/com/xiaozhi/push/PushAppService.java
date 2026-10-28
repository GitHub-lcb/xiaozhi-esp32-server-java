package com.xiaozhi.push;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.common.model.bo.PushMessageBO;
import com.xiaozhi.common.model.req.PushMessageCreateReq;
import com.xiaozhi.common.model.req.PushMessagePageReq;
import com.xiaozhi.common.model.resp.PushMessageResp;
import com.xiaozhi.communication.common.RedisBroadcast;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.push.convert.PushMessageConvert;
import com.xiaozhi.push.service.PushMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 主动推送：把一条通知下发给在线设备，由持有会话的 dialogue 实例播报。
 * <p>
 * 双进程架构下 server 拿不到 WebSocket 会话，下发只能靠 Redis 广播：
 * server 负责选目标、判断在线、落库计数，dialogue 负责真正开口。
 */
@Slf4j
@Service
public class PushAppService {

    @Resource
    private PushMessageService pushMessageService;

    @Resource
    private PushMessageConvert pushMessageConvert;

    @Resource
    private DeviceService deviceService;

    @Resource
    private DeviceChannelService deviceChannelService;

    @Resource
    private RedisBroadcast redisBroadcast;

    public PageResult<PushMessageResp> page(PushMessagePageReq req) {
        return pushMessageService.page(req.getPageNo(), req.getPageSize(), req.getTitle(), req.getState())
            .map(pushMessageConvert::toResp);
    }

    public PushMessageResp create(PushMessageCreateReq req, Integer userId) {
        PushMessageBO bo = new PushMessageBO();
        bo.setTitle(req.getTitle());
        bo.setContent(req.getContent());
        bo.setTargetType(req.getTargetType());
        bo.setTargetValue(req.getTargetValue());
        bo.setCreateUserId(userId);
        return pushMessageConvert.toResp(pushMessageService.create(bo));
    }

    public void delete(Long pushId) {
        pushMessageService.delete(pushId);
    }

    /**
     * 立即发送：只把消息投给在线设备，离线的跳过并在记录里留个数。
     * 离线设备不排队——提醒类消息晚到比不到更糟，需要补发的场景由运营重新发起。
     */
    @Transactional(rollbackFor = Exception.class)
    public PushMessageResp send(Long pushId) {
        PushMessageBO push = pushMessageService.getBO(pushId);
        if (!PushMessageBO.STATE_PENDING.equals(push.getState())) {
            throw new OperationFailedException("只有待发送的推送可以发送");
        }
        List<String> deviceIds = resolveTargetDevices(push);
        if (deviceIds.isEmpty()) {
            throw new OperationFailedException("没有解析到任何目标设备，检查设备ID或用户是否绑定了设备");
        }
        Map<String, DeviceChannelBO> channels = deviceChannelService.mapByDeviceIds(deviceIds);
        int sent = 0;
        int offline = 0;
        for (String deviceId : deviceIds) {
            DeviceChannelBO channel = channels.get(deviceId);
            if (channel != null && channel.isOnline()) {
                redisBroadcast.pushDevice(deviceId, push.getContent(), pushId);
                sent++;
            } else {
                offline++;
            }
        }
        pushMessageService.markSent(pushId, sent, offline, deviceIds.size());
        log.info("主动推送下发完成 pushId={} total={} sent={} offline={}", pushId, deviceIds.size(), sent, offline);
        return pushMessageConvert.toResp(pushMessageService.getBO(pushId));
    }

    /**
     * 目标解析：按设备直接取；按用户取用户名下全部设备。去重并保持顺序。
     */
    private List<String> resolveTargetDevices(PushMessageBO push) {
        Set<String> deviceIds = new LinkedHashSet<>();
        List<String> tokens = splitTargets(push.getTargetValue());
        if (PushMessageBO.TARGET_USER.equals(push.getTargetType())) {
            for (String userIdToken : tokens) {
                try {
                    List<DeviceBO> devices = deviceService.listByUserId(Integer.valueOf(userIdToken));
                    if (devices != null) {
                        devices.stream()
                            .map(DeviceBO::getDeviceId)
                            .filter(StringUtils::hasText)
                            .forEach(deviceIds::add);
                    }
                } catch (NumberFormatException e) {
                    log.warn("推送目标里的用户ID不是数字，已跳过：{}", userIdToken);
                }
            }
        } else {
            deviceIds.addAll(tokens);
        }
        return new ArrayList<>(deviceIds);
    }

    private List<String> splitTargets(String targetValue) {
        if (!StringUtils.hasText(targetValue)) {
            return List.of();
        }
        return Arrays.stream(targetValue.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
    }
}
