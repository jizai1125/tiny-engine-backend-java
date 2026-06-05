package com.tinyengine.it.controller;

import com.tinyengine.it.common.base.Result;
import com.tinyengine.it.common.log.SystemControllerLog;
import com.tinyengine.it.model.dto.AiServiceDto;
import com.tinyengine.it.model.dto.AiServiceUpsertRequest;
import com.tinyengine.it.model.dto.AiUserSettingsDto;
import com.tinyengine.it.service.app.AiModelConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/app-center/api/ai")
@Tag(name = "AIConfig")
public class AiModelConfigController {
    private final AiModelConfigService aiModelConfigService;

    public AiModelConfigController(AiModelConfigService aiModelConfigService) {
        this.aiModelConfigService = aiModelConfigService;
    }

    @SystemControllerLog(description = "query visible ai services")
    @GetMapping("/services")
    public Result<List<AiServiceDto>> getVisibleServices() {
        return Result.success(aiModelConfigService.getVisibleServices());
    }

    @SystemControllerLog(description = "create custom ai service")
    @PostMapping("/services")
    public Result<AiServiceDto> createCustomService(@Valid @RequestBody AiServiceUpsertRequest request) {
        return Result.success(aiModelConfigService.createCustomService(request));
    }

    @SystemControllerLog(description = "update custom ai service")
    @PostMapping("/services/{id}")
    public Result<AiServiceDto> updateCustomService(
        @PathVariable Integer id,
        @Valid @RequestBody AiServiceUpsertRequest request
    ) {
        return Result.success(aiModelConfigService.updateCustomService(id, request));
    }

    @SystemControllerLog(description = "delete custom ai service")
    @DeleteMapping("/services/{id}")
    public Result<Void> deleteCustomService(@PathVariable Integer id) {
        aiModelConfigService.deleteCustomService(id);
        return Result.success();
    }

    @SystemControllerLog(description = "query ai user settings")
    @GetMapping("/settings")
    public Result<AiUserSettingsDto> getUserSettings() {
        return Result.success(aiModelConfigService.getUserSettings());
    }

    @SystemControllerLog(description = "save ai user settings")
    @PostMapping("/settings")
    public Result<AiUserSettingsDto> saveUserSettings(@RequestBody AiUserSettingsDto settings) {
        return Result.success(aiModelConfigService.saveUserSettings(settings));
    }
}
