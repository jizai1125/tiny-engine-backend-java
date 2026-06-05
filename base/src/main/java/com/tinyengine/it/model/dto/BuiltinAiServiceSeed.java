package com.tinyengine.it.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class BuiltinAiServiceSeed {
    @JsonProperty("service_key")
    private String serviceKey;
    private String provider;
    private String label;
    @JsonProperty("base_url")
    private String baseUrl;
    @JsonProperty("allow_empty_api_key")
    private Boolean allowEmptyApiKey;
    private List<AiServiceModelDto> models;
    private Integer sort;
    @JsonProperty("seed_version")
    private String seedVersion;
}
