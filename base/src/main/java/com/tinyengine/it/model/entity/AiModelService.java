package com.tinyengine.it.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tinyengine.it.common.base.BaseEntity;
import com.tinyengine.it.common.handler.ListTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@TableName(value = "t_ai_model_service", autoResultMap = true)
@Schema(name = "AiModelService", description = "AI 模型服务配置")
public class AiModelService extends BaseEntity {
    @Schema(name = "platformId", description = "平台 id")
    @TableField("platform_id")
    private Integer platformId;

    @Schema(name = "scopeType", description = "作用域：PLATFORM/USER")
    @TableField("scope_type")
    private String scopeType;

    @Schema(name = "ownerUserId", description = "归属用户 id")
    @TableField("owner_user_id")
    private String ownerUserId;

    @Schema(name = "serviceKey", description = "服务稳定标识")
    @TableField("service_key")
    private String serviceKey;

    @Schema(name = "provider", description = "提供商标识")
    private String provider;

    @Schema(name = "label", description = "服务展示名称")
    private String label;

    @Schema(name = "baseUrl", description = "模型服务地址")
    @TableField("base_url")
    private String baseUrl;

    @Schema(name = "apiKey", description = "加密后的服务密钥")
    @TableField("api_key")
    private String apiKey;

    @Schema(name = "allowEmptyApiKey", description = "是否允许空 key")
    @TableField("allow_empty_api_key")
    private Boolean allowEmptyApiKey;

    @Schema(name = "modelsJson", description = "模型配置")
    @TableField(value = "models_json", typeHandler = ListTypeHandler.class)
    private List<Map<String, Object>> modelsJson;

    @Schema(name = "builtIn", description = "是否内置服务")
    @TableField("is_built_in")
    private Boolean builtIn;

    @Schema(name = "editable", description = "是否允许前端编辑")
    private Boolean editable;

    @Schema(name = "enabled", description = "是否启用")
    private Boolean enabled;

    @Schema(name = "deprecated", description = "是否已废弃")
    private Boolean deprecated;

    @Schema(name = "seedSpecHash", description = "内置种子定义指纹")
    @TableField("seed_spec_hash")
    private String seedSpecHash;

    @Schema(name = "seedVersion", description = "内置种子版本")
    @TableField("seed_version")
    private String seedVersion;

    @Schema(name = "sort", description = "排序值")
    private Integer sort;
}
