package com.tinyengine.it.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tinyengine.it.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_ai_user_setting")
@Schema(name = "AiUserSetting", description = "用户 AI 模型设置")
public class AiUserSetting extends BaseEntity {
    @Schema(name = "platformId", description = "平台 id")
    @TableField("platform_id")
    private Integer platformId;

    @Schema(name = "userId", description = "用户 id")
    @TableField("user_id")
    private String userId;

    @Schema(name = "defaultServiceKey", description = "默认模型服务标识")
    @TableField("default_service_key")
    private String defaultServiceKey;

    @Schema(name = "defaultModelName", description = "默认模型名")
    @TableField("default_model_name")
    private String defaultModelName;

    @Schema(name = "quickServiceKey", description = "快速模型服务标识")
    @TableField("quick_service_key")
    private String quickServiceKey;

    @Schema(name = "quickModelName", description = "快速模型名")
    @TableField("quick_model_name")
    private String quickModelName;
}
