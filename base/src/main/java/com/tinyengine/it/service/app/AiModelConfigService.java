package com.tinyengine.it.service.app;

import com.tinyengine.it.model.dto.AiServiceDto;
import com.tinyengine.it.model.dto.AiServiceUpsertRequest;
import com.tinyengine.it.model.dto.AiUserSettingsDto;
import com.tinyengine.it.model.dto.ResolvedAiService;

import java.util.List;

public interface AiModelConfigService {
    List<AiServiceDto> getVisibleServices();

    AiServiceDto createCustomService(AiServiceUpsertRequest request);

    AiServiceDto updateCustomService(Integer id, AiServiceUpsertRequest request);

    void deleteCustomService(Integer id);

    AiUserSettingsDto getUserSettings();

    AiUserSettingsDto saveUserSettings(AiUserSettingsDto settings);

    ResolvedAiService resolveChatService(String serviceKey, String modelName);
}
