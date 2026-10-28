package com.xiaozhi.voiceprint.service;

import com.xiaozhi.common.model.bo.VoiceprintBO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VoiceprintService {

    List<VoiceprintBO> listByUser(Integer userId);

    VoiceprintBO register(VoiceprintBO bo);

    void delete(Long voiceprintId, Integer userId);

    /** 取某个用户下全部启用的声纹，供识别时比对 */
    List<VoiceprintBO> listEnabled(Integer userId);

    Optional<VoiceprintBO> getBO(Long voiceprintId);
}
