package com.xiaozhi.voiceclone.dal.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaozhi.common.dal.mysql.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_voice_clone")
public class VoiceCloneDO extends BaseDO {

    @TableId(value = "cloneId", type = IdType.AUTO)
    private Long cloneId;

    private Integer userId;
    private String voiceName;
    private String provider;
    private String voiceId;
    private String samplePath;
    private String state;
    private String errorMsg;
}
