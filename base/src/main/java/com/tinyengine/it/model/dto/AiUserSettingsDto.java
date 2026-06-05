package com.tinyengine.it.model.dto;

import lombok.Data;

@Data
public class AiUserSettingsDto {
    private AiModelSelectionDto defaultModel;
    private AiModelSelectionDto quickModel;
}
