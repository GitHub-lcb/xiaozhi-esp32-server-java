package com.xiaozhi.stackchan.community;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xiaozhi.common.model.req.StackChanFriendAddReq;
import com.xiaozhi.stackchan.StackChanDeviceAuth;
import com.xiaozhi.stackchan.model.resp.StackChanApiResponse;
import com.xiaozhi.stackchan.service.StackChanFriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * StackChan 固件/App 好友接口（对应 Go 版后端 {@code api/friend/v1}）。
 * <p>
 * 加好友由当前鉴权设备发起，{@code friendMac} 会被归一化成小写冒号格式；
 * 不能加自己，双向已存在按幂等成功处理，返回文案与 Go 版一致。
 */
@RestController
@RequestMapping("/api/stackChan")
@Tag(name = "StackChan 好友接口", description = "固件/App 侧加好友，设备身份由 Device-Id + HMAC token 确定")
public class StackChanFriendController {

    @Resource
    private StackChanDeviceAuth stackChanDeviceAuth;

    @Resource
    private StackChanFriendService stackChanFriendService;

    @SaIgnore
    @PostMapping("/friend")
    @ResponseBody
    @Operation(summary = "加好友", description = "JSON 入参 {\"friendMac\":\"...\"}；不能加自己，重复添加幂等成功")
    public StackChanApiResponse<String> addFriend(@Valid @RequestBody StackChanFriendAddReq req,
                                                  HttpServletRequest request) {
        String mac = stackChanDeviceAuth.authenticate(request);
        stackChanFriendService.add(mac, req.getFriendMac());
        return StackChanApiResponse.success("Successfully added a friend");
    }
}
