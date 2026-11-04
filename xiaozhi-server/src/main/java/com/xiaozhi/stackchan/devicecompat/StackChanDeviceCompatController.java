package com.xiaozhi.stackchan.devicecompat;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.req.DeviceUpdateReq;
import com.xiaozhi.device.DeviceAppService;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.stackchan.StackChanDeviceAuth;
import com.xiaozhi.stackchan.devicecompat.model.req.StackChanDeviceNameReq;
import com.xiaozhi.stackchan.devicecompat.model.resp.StackChanDeviceCreateResp;
import com.xiaozhi.stackchan.model.resp.StackChanApiResponse;
import com.xiaozhi.stackchan.model.resp.StackChanDeviceInfoResp;
import com.xiaozhi.stackchan.ws.StackChanDeviceConnection;
import com.xiaozhi.stackchan.ws.StackChanWsSessionRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * StackChan 设备侧 v1 接口里 P0 未覆盖的四个：改设备名（两个路径）、建档、随机在线设备列表。
 * <p>
 * 路径都挂在 {@code /api/stackChan} 下，与 Go 版设备组一致；设备身份由
 * {@link StackChanDeviceAuth}（Device-Id + HMAC token）确定，响应保持 GoFrame 的
 * {@code {"code":0,"message":"","data":...}} 结构，固件/App 不用改解析。
 * <p>
 * {@code GET /device/info} 仍由 {@link com.xiaozhi.stackchan.StackChanController} 提供（P0 已有，
 * 本次只扩展返回字段），本控制器不重复映射，避免 Spring 报路径冲突。
 */
@Slf4j
@RestController
@RequestMapping("/api/stackChan")
@Tag(name = "StackChan 设备 v1 兼容接口", description = "固件侧设备改名/建档/随机在线设备列表，设备身份由 Device-Id + HMAC token 确定")
public class StackChanDeviceCompatController {

    /** Go 版 {@code GetRandomDeviceReq.PageSize} 的默认值，也是 <=0 时的兜底值 */
    private static final int DEFAULT_RANDOM_PAGE_SIZE = 6;

    /** 与 {@code sys_device.deviceName varchar(100)} 对齐，超长提前报业务错误而不是撞数据库截断 */
    private static final int DEVICE_NAME_MAX_LENGTH = 100;

    /** 与 App 侧 StackChanDeviceResp.bindTime、GET /device/info 保持同一时间格式 */
    private static final DateTimeFormatter BIND_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private StackChanDeviceAuth stackChanDeviceAuth;

    @Resource
    private DeviceService deviceService;

    @Resource
    private DeviceAppService deviceAppService;

    @Resource
    private StackChanWsSessionRegistry stackChanWsSessionRegistry;

    /**
     * Go 版 {@code PUT /device/info}：只改 name，data 固定字符串 "Update successful"。
     */
    @SaIgnore
    @PutMapping("/device/info")
    @ResponseBody
    @Operation(summary = "修改当前设备信息", description = "按 Device-Id 定位当前设备，只改 deviceName；返回 \"Update successful\"")
    public StackChanApiResponse<String> updateDeviceInfo(
        @RequestBody(required = false) StackChanDeviceNameReq req, HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        applyDeviceName(requireDevice(mac), req == null ? null : req.getName());
        return StackChanApiResponse.success("Update successful");
    }

    /**
     * Go 版 {@code PUT /device}：只改 name，data 是空对象。这里用 {@code StackChanApiResponse<Void>} 返回
     * {@code data:null}，与空对象一样没有字段契约，客户端只认 code。
     */
    @SaIgnore
    @PutMapping("/device")
    @ResponseBody
    @Operation(summary = "更新当前设备", description = "按 Device-Id 定位当前设备，只改 deviceName；data 为空")
    public StackChanApiResponse<Void> updateDevice(
        @RequestBody(required = false) StackChanDeviceNameReq req, HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        applyDeviceName(requireDevice(mac), req == null ? null : req.getName());
        return StackChanApiResponse.success(null);
    }

    /**
     * Go 版 {@code POST /device}：插入一条设备行并返回自增 id。
     * <p>
     * Java 侧设备由 OTA 自动建档，且主键是 deviceId 字符串、没有自增 id，所以不新建行：
     * 设备已存在则更新名称并幂等返回 {@code {"id":0}}；设备不存在时提示先走 OTA，避免建出没有归属用户的孤儿设备。
     */
    @SaIgnore
    @PostMapping("/device")
    @ResponseBody
    @Operation(summary = "当前设备建档（幂等）", description = "设备已存在时更新名称并返回 id=0；不存在时提示先通过 OTA 上报自动建档")
    public StackChanApiResponse<StackChanDeviceCreateResp> createDevice(
        @RequestBody(required = false) StackChanDeviceNameReq req, HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        DeviceBO device = deviceService.getBO(mac);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在，请先通过 OTA 上报完成自动建档");
        }
        applyDeviceName(device, req == null ? null : req.getName());

        StackChanDeviceCreateResp resp = new StackChanDeviceCreateResp();
        // sys_device 主键是 deviceId 字符串，没有 Go 版的自增行 id；0 表示"复用已有行"
        resp.setId(0L);
        return StackChanApiResponse.success(resp);
    }

    /**
     * Go 版 {@code GET /device/randomList?pageSize=6}：从伴侣 WS 在线设备池取 MAC（排除当前设备），
     * 洗牌后取前 pageSize 个，再按 MAC 批量查设备表，返回 {@code [{mac,name,uid,bind_time}]}。
     * <p>
     * 与 Go 一致：数据库里查不到的设备（例如池里有连接但档案已删）直接跳过，不补占位。
     */
    @SaIgnore
    @GetMapping("/device/randomList")
    @ResponseBody
    @Operation(summary = "随机在线设备列表",
        description = "从伴侣 WS 在线池随机挑选不包含当前设备的在线设备，最多 pageSize 个；默认 pageSize=6")
    public StackChanApiResponse<List<StackChanDeviceInfoResp>> randomList(
        @RequestParam(value = "pageSize", required = false, defaultValue = "6") Integer pageSize,
        HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        int size = pageSize == null || pageSize <= 0 ? DEFAULT_RANDOM_PAGE_SIZE : pageSize;

        List<String> onlineMacs = new ArrayList<>();
        for (StackChanDeviceConnection connection : stackChanWsSessionRegistry.allDevices()) {
            if (connection == null || connection.getMac() == null) {
                continue;
            }
            // 连接池按 MAC 单连接持有，断线后对象会留到超时清理，isOnline() 才是当前的在线口径
            if (!connection.isOnline() || mac.equals(connection.getMac())) {
                continue;
            }
            onlineMacs.add(connection.getMac());
        }
        if (onlineMacs.isEmpty()) {
            return StackChanApiResponse.success(List.of());
        }

        Collections.shuffle(onlineMacs);
        List<String> pickedMacs = onlineMacs.size() > size
            ? new ArrayList<>(onlineMacs.subList(0, size)) : onlineMacs;

        Map<String, DeviceBO> deviceMap = deviceService.listByDeviceIds(pickedMacs).stream()
            .collect(Collectors.toMap(DeviceBO::getDeviceId, Function.identity(), (first, second) -> first));
        List<StackChanDeviceInfoResp> result = new ArrayList<>(pickedMacs.size());
        for (String pickedMac : pickedMacs) {
            DeviceBO device = deviceMap.get(pickedMac);
            if (device == null) {
                continue;
            }
            result.add(toDeviceInfoResp(device));
        }
        return StackChanApiResponse.success(result);
    }

    /** 设备侧写接口都先确认设备档案存在：不存在说明还没 OTA 建档，改了也无处落库 */
    private DeviceBO requireDevice(String mac) {
        DeviceBO device = deviceService.getBO(mac);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        return device;
    }

    /**
     * 按 Go 语义改名：name 为空视为不修改；与现值相同直接跳过，避免每次请求都写一次库。
     */
    private void applyDeviceName(DeviceBO current, String rawName) {
        if (!StringUtils.hasText(rawName)) {
            return;
        }
        String name = rawName.trim();
        if (name.length() > DEVICE_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "设备名称过长（最多 " + DEVICE_NAME_MAX_LENGTH + " 个字符）");
        }
        if (name.equals(current.getDeviceName())) {
            return;
        }
        DeviceUpdateReq updateReq = new DeviceUpdateReq();
        updateReq.setDeviceName(name);
        deviceAppService.update(current.getDeviceId(), updateReq);
        log.info("StackChan 设备名已更新, mac={}, name={}", current.getDeviceId(), name);
    }

    private StackChanDeviceInfoResp toDeviceInfoResp(DeviceBO device) {
        StackChanDeviceInfoResp resp = new StackChanDeviceInfoResp();
        resp.setMac(device.getDeviceId());
        resp.setName(StringUtils.hasText(device.getDeviceName()) ? device.getDeviceName() : "");
        resp.setUid(device.getUserId());
        resp.setBindTime(device.getCreateTime() == null ? "" : BIND_TIME_FORMATTER.format(device.getCreateTime()));
        return resp;
    }
}
