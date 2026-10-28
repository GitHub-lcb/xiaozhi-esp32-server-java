package com.xiaozhi.voiceclone.service;

import com.xiaozhi.common.model.bo.VoiceCloneBO;

import java.util.List;

public interface VoiceCloneService {

    List<VoiceCloneBO> listByUser(Integer userId);

    VoiceCloneBO create(VoiceCloneBO bo);

    /** 回填服务商结果：成功写入 voiceId，失败写入原因 */
    void markResult(Long cloneId, String voiceId, String errorMsg);

    void delete(Long cloneId, Integer userId);

    List<VoiceCloneBO> listReady(Integer userId);
}
