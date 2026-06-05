package com.tinyengine.it.model.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AiServiceModelDto {
    private String name;
    private String label;
    private Map<String, Object> capabilities;
}
