package com.tinyengine.it.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ResolvedAiService {
    private String serviceKey;
    private String provider;
    private String label;
    private String baseUrl;
    private String apiKey;
    private Boolean allowEmptyApiKey;
    private Boolean enabled;
    private Boolean deprecated;
    private String scopeType;
    private String ownerUserId;
    private Boolean builtIn;
    private String modelName;
    private List<AiServiceModelDto> models;
}
