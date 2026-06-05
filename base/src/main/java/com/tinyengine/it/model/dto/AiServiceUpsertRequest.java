package com.tinyengine.it.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiServiceUpsertRequest {
    private String provider;
    private String label;
    private String baseUrl;
    private String apiKey;
    private Boolean allowEmptyApiKey;
    private List<AiServiceModelDto> models;
}
