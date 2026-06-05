package com.tinyengine.it.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AiServiceDto {
    private Integer id;
    private String serviceKey;
    private String provider;
    private String label;
    private String baseUrl;
    private List<AiServiceModelDto> models;
    @JsonProperty("isBuiltIn")
    private Boolean builtIn;
    private String scopeType;
    private Boolean editable;
    private Boolean enabled;
    private Boolean deprecated;
    private Boolean hasApiKey;
    private Boolean allowEmptyApiKey;
}
