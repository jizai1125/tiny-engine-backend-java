package com.tinyengine.it.service.app.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tinyengine.it.common.context.LoginUserContext;
import com.tinyengine.it.common.exception.ServiceException;
import com.tinyengine.it.common.utils.JsonUtils;
import com.tinyengine.it.common.utils.SM4Utils;
import com.tinyengine.it.config.AiSecretConfig;
import com.tinyengine.it.mapper.AiModelServiceMapper;
import com.tinyengine.it.mapper.AiUserSettingMapper;
import com.tinyengine.it.model.dto.AiModelSelectionDto;
import com.tinyengine.it.model.dto.AiServiceDto;
import com.tinyengine.it.model.dto.AiServiceModelDto;
import com.tinyengine.it.model.dto.AiServiceUpsertRequest;
import com.tinyengine.it.model.dto.AiUserSettingsDto;
import com.tinyengine.it.model.dto.BuiltinAiServiceSeed;
import com.tinyengine.it.model.dto.ResolvedAiService;
import com.tinyengine.it.model.entity.AiModelService;
import com.tinyengine.it.model.entity.AiUserSetting;
import com.tinyengine.it.service.app.AiModelConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AiModelConfigServiceImpl implements AiModelConfigService {
    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_USER = "USER";

    private final AiModelServiceMapper aiModelServiceMapper;
    private final AiUserSettingMapper aiUserSettingMapper;
    private final LoginUserContext loginUserContext;
    private final AiSecretConfig aiSecretConfig;
    private final Set<String> syncedScopes = ConcurrentHashMap.newKeySet();

    private List<BuiltinAiServiceSeed> builtinSeeds = List.of();

    public AiModelConfigServiceImpl(
        AiModelServiceMapper aiModelServiceMapper,
        AiUserSettingMapper aiUserSettingMapper,
        LoginUserContext loginUserContext,
        AiSecretConfig aiSecretConfig
    ) {
        this.aiModelServiceMapper = aiModelServiceMapper;
        this.aiUserSettingMapper = aiUserSettingMapper;
        this.loginUserContext = loginUserContext;
        this.aiSecretConfig = aiSecretConfig;
    }

    @PostConstruct
    public void loadBuiltinSeeds() {
        ClassPathResource resource = new ClassPathResource("ai-services/builtin-services.json");
        if (!resource.exists()) {
            log.warn("builtin ai service seed file not found");
            builtinSeeds = List.of();
            return;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            builtinSeeds = JsonUtils.decode(
                inputStream.readAllBytes(),
                new TypeReference<List<BuiltinAiServiceSeed>>() {}
            );
        } catch (IOException e) {
            throw new ServiceException("500", "Failed to load builtin AI services seed");
        }
    }

    @Override
    public List<AiServiceDto> getVisibleServices() {
        ensureBuiltinServicesSynced();
        return listVisibleServices().stream().map(this::toServiceDto).toList();
    }

    @Override
    @Transactional
    public AiServiceDto createCustomService(AiServiceUpsertRequest request) {
        ensureAuthenticatedUser();
        validateCustomServiceRequest(request);

        AiModelService service = new AiModelService();
        service.setPlatformId(getPlatformId());
        service.setTenantId(getTenantId());
        service.setScopeType(SCOPE_USER);
        service.setOwnerUserId(getUserId());
        service.setServiceKey(generateCustomServiceKey());
        service.setProvider(StringUtils.hasText(request.getProvider()) ? request.getProvider() : "custom");
        service.setLabel(request.getLabel().trim());
        service.setBaseUrl(request.getBaseUrl().trim());
        service.setApiKey(normalizeApiKey(request.getApiKey()));
        service.setAllowEmptyApiKey(Boolean.TRUE.equals(request.getAllowEmptyApiKey()));
        service.setModelsJson(toModelMaps(request.getModels()));
        service.setBuiltIn(false);
        service.setEditable(true);
        service.setEnabled(true);
        service.setDeprecated(false);
        service.setSort(0);
        aiModelServiceMapper.insert(service);
        return toServiceDto(service);
    }

    @Override
    @Transactional
    public AiServiceDto updateCustomService(Integer id, AiServiceUpsertRequest request) {
        validateCustomServiceRequest(request);

        AiModelService service = getOwnedCustomService(id);
        service.setProvider(StringUtils.hasText(request.getProvider()) ? request.getProvider() : service.getProvider());
        service.setLabel(request.getLabel().trim());
        service.setBaseUrl(request.getBaseUrl().trim());
        service.setAllowEmptyApiKey(Boolean.TRUE.equals(request.getAllowEmptyApiKey()));
        service.setModelsJson(toModelMaps(request.getModels()));
        if (request.getApiKey() != null) {
            service.setApiKey(normalizeApiKey(request.getApiKey()));
        }
        aiModelServiceMapper.updateById(service);
        return toServiceDto(service);
    }

    @Override
    @Transactional
    public void deleteCustomService(Integer id) {
        AiModelService service = getOwnedCustomService(id);
        aiModelServiceMapper.deleteById(service.getId());
    }

    @Override
    public AiUserSettingsDto getUserSettings() {
        ensureBuiltinServicesSynced();
        List<AiModelService> visibleServices = listVisibleServices();
        AiUserSetting entity = hasAuthenticatedUser() ? findUserSetting() : null;
        AiModelSelectionDto defaultSelection = validateOrFallbackSelection(
            entity == null ? null : toSelection(entity.getDefaultServiceKey(), entity.getDefaultModelName()),
            visibleServices,
            false,
            false
        );
        AiModelSelectionDto quickSelection = validateOrFallbackSelection(
            entity == null ? null : toSelection(entity.getQuickServiceKey(), entity.getQuickModelName()),
            visibleServices,
            true,
            true
        );

        AiUserSettingsDto settings = new AiUserSettingsDto();
        settings.setDefaultModel(defaultSelection);
        settings.setQuickModel(quickSelection);
        return settings;
    }

    @Override
    @Transactional
    public AiUserSettingsDto saveUserSettings(AiUserSettingsDto settings) {
        ensureAuthenticatedUser();
        ensureBuiltinServicesSynced();
        List<AiModelService> visibleServices = listVisibleServices();
        AiModelSelectionDto defaultSelection = validateSelection(settings.getDefaultModel(), visibleServices, false);
        AiModelSelectionDto quickSelection = validateSelection(settings.getQuickModel(), visibleServices, true);

        AiUserSetting entity = findUserSetting();
        if (entity == null) {
            entity = new AiUserSetting();
            entity.setPlatformId(getPlatformId());
            entity.setTenantId(getTenantId());
            entity.setUserId(getUserId());
        }

        entity.setDefaultServiceKey(defaultSelection.getServiceKey());
        entity.setDefaultModelName(defaultSelection.getModelName());
        entity.setQuickServiceKey(quickSelection.getServiceKey());
        entity.setQuickModelName(quickSelection.getModelName());

        if (entity.getId() == null) {
            aiUserSettingMapper.insert(entity);
        } else {
            aiUserSettingMapper.updateById(entity);
        }

        AiUserSettingsDto result = new AiUserSettingsDto();
        result.setDefaultModel(defaultSelection);
        result.setQuickModel(quickSelection);
        return result;
    }

    @Override
    public ResolvedAiService resolveChatService(String serviceKey, String modelName) {
        if (!StringUtils.hasText(serviceKey)) {
            return null;
        }

        String currentUserId = getUserId();
        if (!StringUtils.hasText(currentUserId) && serviceKey.startsWith("custom_")) {
            throw new ServiceException("401", "Login required for custom AI service");
        }

        ensureBuiltinServicesSynced();
        AiModelService service = findVisibleServiceByKey(serviceKey);
        if (service == null) {
            throw new ServiceException("400", "AI service not found: " + serviceKey);
        }
        if (!Boolean.TRUE.equals(service.getEnabled())) {
            throw new ServiceException("400", "AI service is disabled: " + serviceKey);
        }
        if (Boolean.TRUE.equals(service.getDeprecated())) {
            throw new ServiceException("400", "AI service is deprecated: " + serviceKey);
        }
        if (!Boolean.TRUE.equals(service.getAllowEmptyApiKey()) && !StringUtils.hasText(service.getApiKey())) {
            throw new ServiceException("400", "AI service API key is not configured: " + service.getLabel());
        }

        List<AiServiceModelDto> models = toModelDtos(service.getModelsJson());
        String resolvedModelName = resolveModelName(models, modelName);
        if (!StringUtils.hasText(resolvedModelName)) {
            throw new ServiceException("400", "AI service has no available model: " + serviceKey);
        }

        ResolvedAiService resolved = new ResolvedAiService();
        resolved.setServiceKey(service.getServiceKey());
        resolved.setProvider(service.getProvider());
        resolved.setLabel(service.getLabel());
        resolved.setBaseUrl(service.getBaseUrl());
        resolved.setApiKey(service.getApiKey());
        resolved.setAllowEmptyApiKey(service.getAllowEmptyApiKey());
        resolved.setEnabled(service.getEnabled());
        resolved.setDeprecated(service.getDeprecated());
        resolved.setScopeType(service.getScopeType());
        resolved.setOwnerUserId(service.getOwnerUserId());
        resolved.setBuiltIn(service.getBuiltIn());
        resolved.setModels(models);
        resolved.setModelName(resolvedModelName);
        return resolved;
    }

    private void ensureBuiltinServicesSynced() {
        String syncKey = getTenantId() + ":" + getPlatformId();
        if (syncedScopes.contains(syncKey)) {
            return;
        }
        synchronized (this) {
            if (syncedScopes.contains(syncKey)) {
                return;
            }
            syncBuiltinAiServices();
            syncedScopes.add(syncKey);
        }
    }

    private void syncBuiltinAiServices() {
        if (builtinSeeds.isEmpty()) {
            return;
        }

        String tenantId = getTenantId();
        Integer platformId = getPlatformId();
        List<AiModelService> existingBuiltins = aiModelServiceMapper.selectList(
            new LambdaQueryWrapper<AiModelService>()
                .eq(AiModelService::getTenantId, tenantId)
                .eq(AiModelService::getPlatformId, platformId)
                .eq(AiModelService::getScopeType, SCOPE_PLATFORM)
        );

        Map<String, AiModelService> existingByKey = existingBuiltins.stream()
            .collect(Collectors.toMap(AiModelService::getServiceKey, item -> item, (left, right) -> left));

        for (BuiltinAiServiceSeed seed : builtinSeeds) {
            String seedHash = buildSeedSpecHash(seed);
            AiModelService current = existingByKey.remove(seed.getServiceKey());
            if (current == null) {
                AiModelService created = new AiModelService();
                created.setTenantId(tenantId);
                created.setPlatformId(platformId);
                created.setScopeType(SCOPE_PLATFORM);
                created.setOwnerUserId("");
                created.setServiceKey(seed.getServiceKey());
                created.setProvider(seed.getProvider());
                created.setLabel(seed.getLabel());
                created.setBaseUrl(seed.getBaseUrl());
                created.setApiKey("");
                created.setAllowEmptyApiKey(Boolean.TRUE.equals(seed.getAllowEmptyApiKey()));
                created.setModelsJson(toModelMaps(seed.getModels()));
                created.setBuiltIn(true);
                created.setEditable(false);
                created.setEnabled(true);
                created.setDeprecated(false);
                created.setSeedSpecHash(seedHash);
                created.setSeedVersion(seed.getSeedVersion());
                created.setSort(seed.getSort() == null ? 0 : seed.getSort());
                aiModelServiceMapper.insert(created);
                continue;
            }

            current.setBuiltIn(true);
            current.setEditable(false);
            current.setDeprecated(false);
            current.setEnabled(current.getEnabled() == null ? true : current.getEnabled());
            current.setSeedVersion(seed.getSeedVersion());
            current.setSort(seed.getSort() == null ? 0 : seed.getSort());

            if (!Objects.equals(current.getSeedSpecHash(), seedHash)) {
                current.setProvider(seed.getProvider());
                current.setLabel(seed.getLabel());
                current.setBaseUrl(seed.getBaseUrl());
                current.setAllowEmptyApiKey(Boolean.TRUE.equals(seed.getAllowEmptyApiKey()));
                current.setModelsJson(toModelMaps(seed.getModels()));
                current.setSeedSpecHash(seedHash);
            }

            aiModelServiceMapper.updateById(current);
        }

        for (AiModelService removed : existingByKey.values()) {
            if (!Boolean.TRUE.equals(removed.getDeprecated())) {
                removed.setDeprecated(true);
                aiModelServiceMapper.updateById(removed);
            }
        }
    }

    private AiUserSetting findUserSetting() {
        ensureAuthenticatedUser();
        return aiUserSettingMapper.selectOne(
            new LambdaQueryWrapper<AiUserSetting>()
                .eq(AiUserSetting::getTenantId, getTenantId())
                .eq(AiUserSetting::getPlatformId, getPlatformId())
                .eq(AiUserSetting::getUserId, getUserId())
                .last("limit 1")
        );
    }

    private List<AiModelService> listVisibleServices() {
        String userId = getUserId();
        QueryWrapper<AiModelService> query = new QueryWrapper<AiModelService>()
            .eq("tenant_id", getTenantId())
            .eq("platform_id", getPlatformId())
            .and(wrapper -> {
                wrapper.eq("scope_type", SCOPE_PLATFORM);
                if (StringUtils.hasText(userId)) {
                    wrapper.or().nested(nested -> nested.eq("scope_type", SCOPE_USER).eq("owner_user_id", userId));
                }
            });

        List<AiModelService> services = aiModelServiceMapper.selectList(query);

        return services.stream()
            .sorted(Comparator
                .comparing((AiModelService item) -> !Boolean.TRUE.equals(item.getBuiltIn()))
                .thenComparing(item -> item.getSort() == null ? 0 : item.getSort())
                .thenComparing(item -> item.getLabel() == null ? "" : item.getLabel(), String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private AiModelService findVisibleServiceByKey(String serviceKey) {
        return listVisibleServices().stream()
            .filter(item -> Objects.equals(item.getServiceKey(), serviceKey))
            .findFirst()
            .orElse(null);
    }

    private AiModelService getOwnedCustomService(Integer id) {
        ensureAuthenticatedUser();
        AiModelService service = aiModelServiceMapper.selectOne(
            new LambdaQueryWrapper<AiModelService>()
                .eq(AiModelService::getId, id)
                .eq(AiModelService::getTenantId, getTenantId())
                .eq(AiModelService::getPlatformId, getPlatformId())
                .eq(AiModelService::getScopeType, SCOPE_USER)
                .eq(AiModelService::getOwnerUserId, getUserId())
                .last("limit 1")
        );

        if (service == null) {
            throw new ServiceException("404", "Custom AI service not found: " + id);
        }
        return service;
    }

    private AiServiceDto toServiceDto(AiModelService service) {
        AiServiceDto dto = new AiServiceDto();
        dto.setId(service.getId());
        dto.setServiceKey(service.getServiceKey());
        dto.setProvider(service.getProvider());
        dto.setLabel(service.getLabel());
        dto.setBaseUrl(service.getBaseUrl());
        dto.setModels(toModelDtos(service.getModelsJson()));
        dto.setBuiltIn(service.getBuiltIn());
        dto.setScopeType(service.getScopeType());
        dto.setEditable(service.getEditable());
        dto.setEnabled(service.getEnabled());
        dto.setDeprecated(service.getDeprecated());
        dto.setHasApiKey(StringUtils.hasText(service.getApiKey()));
        dto.setAllowEmptyApiKey(service.getAllowEmptyApiKey());
        return dto;
    }

    private List<AiServiceModelDto> toModelDtos(List<Map<String, Object>> models) {
        if (models == null || models.isEmpty()) {
            return List.of();
        }
        return models.stream()
            .map(item -> JsonUtils.convertValue(item, AiServiceModelDto.class))
            .toList();
    }

    private List<Map<String, Object>> toModelMaps(List<AiServiceModelDto> models) {
        if (models == null || models.isEmpty()) {
            return List.of();
        }
        return models.stream()
            .map(this::normalizeModel)
            .toList();
    }

    private Map<String, Object> normalizeModel(AiServiceModelDto model) {
        if (!StringUtils.hasText(model.getName()) || !StringUtils.hasText(model.getLabel())) {
            throw new ServiceException("400", "AI model name and label are required");
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("name", model.getName().trim());
        normalized.put("label", model.getLabel().trim());
        normalized.put("capabilities", model.getCapabilities() == null ? Map.of() : model.getCapabilities());
        return normalized;
    }

    private void validateCustomServiceRequest(AiServiceUpsertRequest request) {
        if (!StringUtils.hasText(request.getLabel())) {
            throw new ServiceException("400", "AI service label is required");
        }
        if (!StringUtils.hasText(request.getBaseUrl())) {
            throw new ServiceException("400", "AI service baseUrl is required");
        }
        if (request.getModels() == null || request.getModels().isEmpty()) {
            throw new ServiceException("400", "AI service models are required");
        }
        request.getModels().forEach(this::normalizeModel);
    }

    private AiModelSelectionDto validateSelection(
        AiModelSelectionDto selection,
        List<AiModelService> visibleServices,
        boolean allowEmpty
    ) {
        if (selection == null || !StringUtils.hasText(selection.getServiceKey()) || !StringUtils.hasText(selection.getModelName())) {
            if (allowEmpty) {
                return toSelection("", "");
            }
            throw new ServiceException("400", "Default AI model is required");
        }

        AiModelService service = visibleServices.stream()
            .filter(item -> Objects.equals(item.getServiceKey(), selection.getServiceKey()))
            .findFirst()
            .orElseThrow(() -> new ServiceException("400", "AI service is not visible: " + selection.getServiceKey()));

        if (!Boolean.TRUE.equals(service.getEnabled()) || Boolean.TRUE.equals(service.getDeprecated())) {
            throw new ServiceException("400", "AI service is not available: " + service.getServiceKey());
        }

        boolean modelExists = toModelDtos(service.getModelsJson()).stream()
            .anyMatch(item -> Objects.equals(item.getName(), selection.getModelName()));
        if (!modelExists) {
            throw new ServiceException("400", "AI model is not available: " + selection.getModelName());
        }
        return toSelection(selection.getServiceKey(), selection.getModelName());
    }

    private AiModelSelectionDto validateOrFallbackSelection(
        AiModelSelectionDto selection,
        List<AiModelService> visibleServices,
        boolean preferCompact,
        boolean allowEmpty
    ) {
        try {
            return validateSelection(selection, visibleServices, allowEmpty);
        } catch (ServiceException ignored) {
            return buildFallbackSelection(visibleServices, preferCompact);
        }
    }

    private AiModelSelectionDto buildFallbackSelection(List<AiModelService> visibleServices, boolean preferCompact) {
        List<AiModelService> builtins = visibleServices.stream()
            .filter(item -> Boolean.TRUE.equals(item.getBuiltIn()))
            .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
            .filter(item -> !Boolean.TRUE.equals(item.getDeprecated()))
            .toList();

        for (AiModelService service : builtins) {
            List<AiServiceModelDto> models = toModelDtos(service.getModelsJson());
            if (preferCompact) {
                AiServiceModelDto compactModel = models.stream()
                    .filter(item -> isCompactModel(item.getCapabilities()))
                    .findFirst()
                    .orElse(null);
                if (compactModel != null) {
                    return toSelection(service.getServiceKey(), compactModel.getName());
                }
            }

            if (!models.isEmpty()) {
                return toSelection(service.getServiceKey(), models.get(0).getName());
            }
        }

        return toSelection("", "");
    }

    private boolean isCompactModel(Map<String, Object> capabilities) {
        if (capabilities == null) {
            return false;
        }
        Object compact = capabilities.get("compact");
        return compact instanceof Boolean && (Boolean) compact;
    }

    private AiModelSelectionDto toSelection(String serviceKey, String modelName) {
        AiModelSelectionDto selection = new AiModelSelectionDto();
        selection.setServiceKey(serviceKey == null ? "" : serviceKey);
        selection.setModelName(modelName == null ? "" : modelName);
        return selection;
    }

    private String resolveModelName(List<AiServiceModelDto> models, String preferredModel) {
        if (models == null || models.isEmpty()) {
            return "";
        }
        if (!StringUtils.hasText(preferredModel)) {
            return models.get(0).getName();
        }
        return models.stream().anyMatch(item -> Objects.equals(item.getName(), preferredModel))
            ? preferredModel
            : "";
    }

    private String buildSeedSpecHash(BuiltinAiServiceSeed seed) {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("service_key", seed.getServiceKey());
        definition.put("provider", seed.getProvider());
        definition.put("label", seed.getLabel());
        definition.put("base_url", seed.getBaseUrl());
        definition.put("allow_empty_api_key", seed.getAllowEmptyApiKey());
        definition.put("models", seed.getModels());
        definition.put("sort", seed.getSort());

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(JsonUtils.encode(definition).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashBytes.length * 2);
            for (byte hashByte : hashBytes) {
                builder.append(String.format(Locale.ROOT, "%02x", hashByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("500", "Failed to hash builtin AI service seed");
        }
    }

    private String normalizeApiKey(String apiKey) {
        if (apiKey == null) {
            return null;
        }

        String normalized = apiKey.trim();
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        if (normalized.startsWith("EKEY_")) {
            return normalized;
        }

        try {
            String sm4Key = aiSecretConfig.getRequiredSm4Key();
            return "EKEY_" + SM4Utils.encryptECB(normalized, sm4Key);
        } catch (Exception e) {
            if (e instanceof ServiceException serviceException) {
                throw serviceException;
            }
            throw new ServiceException("500", "Failed to encrypt AI service API key: " + e.getMessage());
        }
    }

    private String generateCustomServiceKey() {
        return "custom_" + UUID.randomUUID().toString().replace("-", "");
    }

    private Integer getPlatformId() {
        return loginUserContext.getPlatformId();
    }

    private String getTenantId() {
        return loginUserContext.getTenantId();
    }

    private String getUserId() {
        return loginUserContext.getLoginUserId();
    }

    private boolean hasAuthenticatedUser() {
        return StringUtils.hasText(getUserId());
    }

    private void ensureAuthenticatedUser() {
        if (!hasAuthenticatedUser()) {
            throw new ServiceException("401", "Login required");
        }
    }
}
