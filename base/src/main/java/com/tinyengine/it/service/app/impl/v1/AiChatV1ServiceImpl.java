/**
 * Copyright (c) 2023 - present TinyEngine Authors.
 * Copyright (c) 2023 - present Huawei Cloud Computing Technologies Co., Ltd.
 *
 * Use of this source code is governed by an MIT-style license.
 *
 * THE OPEN SOURCE SOFTWARE IN THIS PRODUCT IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR PURPOSE. SEE THE APPLICABLE LICENSES FOR MORE DETAILS.
 *
 */

package com.tinyengine.it.service.app.impl.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.tinyengine.it.common.exception.ServiceException;
import com.tinyengine.it.common.log.SystemServiceLog;
import com.tinyengine.it.common.utils.JsonUtils;
import com.tinyengine.it.common.utils.SM4Utils;
import com.tinyengine.it.config.AiSecretConfig;
import com.tinyengine.it.config.OpenAIConfig;
import com.tinyengine.it.model.dto.ChatRequest;
import com.tinyengine.it.model.dto.ResolvedAiService;
import com.tinyengine.it.model.entity.Resource;
import com.tinyengine.it.service.app.AiModelConfigService;
import com.tinyengine.it.service.app.v1.AiChatV1Service;
import com.tinyengine.it.service.material.ResourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The type AiChat v1 service.
 *
 * @since 2025-08-06
 */
@Slf4j
@Service
public class AiChatV1ServiceImpl implements AiChatV1Service {
    private final OpenAIConfig config;
    private final HttpClient httpClient;
    private final AiModelConfigService aiModelConfigService;
    private final AiSecretConfig aiSecretConfig;
    private final ResourceService resourceService;

    @Autowired
    public AiChatV1ServiceImpl(
        OpenAIConfig config,
        AiModelConfigService aiModelConfigService,
        AiSecretConfig aiSecretConfig,
        ResourceService resourceService
    ) {
        this.config = config;
        this.aiModelConfigService = aiModelConfigService;
        this.aiSecretConfig = aiSecretConfig;
        this.resourceService = resourceService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    AiChatV1ServiceImpl(OpenAIConfig config) {
        this(config, null, null, null);
    }

    /**
     * chatCompletion.
     *
     * @param request the request
     * @return Object the Object
     */
    @Override
    @SystemServiceLog(description = "chatCompletion")
    public Object chatCompletion(ChatRequest request) throws Exception {
        ResolvedAiService resolvedAiService = aiModelConfigService.resolveChatService(
            request.getServiceKey(),
            request.getModel()
        );
        if (resolvedAiService == null) {
            throw new ServiceException("400", "AI serviceKey is required");
        }

        request.setModel(resolvedAiService.getModelName());
        request.setBaseUrl(resolvedAiService.getBaseUrl());
        request.setApiKey(resolvedAiService.getApiKey());
        request.setMessages(resolveLocalResourceImageUrls(request.getMessages()));

        String requestBody = buildRequestBody(request);
        String encryptApiKey = request.getApiKey();
        String apiKey = StringUtils.hasText(encryptApiKey) ? getApiKey(encryptApiKey) : "";
        String baseUrl = request.getBaseUrl();

        // 规范化URL处理
        String normalizedUrl = normalizeApiUrl(baseUrl);

        // 对最终请求 URL 做安全校验（在 normalize 之后，确保校验的是真正发出的地址）
        validateFinalUrl(normalizedUrl);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(normalizedUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody));
        if (StringUtils.hasText(apiKey)) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }
        if (request.isStream()) {
            requestBuilder.header("Accept", "text/event-stream");
            return processStreamResponse(requestBuilder);
        } else {
            return processStandardResponse(requestBuilder);
        }
    }

    /**
     * get token.
     *
     * @param apiKey the apiKey
     * @return token the token
     */
    @Override
    public String getToken(String apiKey) throws Exception {
        String sm4Key = aiSecretConfig.getRequiredSm4Key();
        String encrypt = SM4Utils.encryptECB(apiKey, sm4Key);
        return "EKEY_"+ encrypt;
    }

    /**
     * 规范化API URL，兼容不同厂商
     */
    private String normalizeApiUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = config.getBaseUrl();
        }
        baseUrl = baseUrl.trim();

        if (baseUrl.contains("/chat/completions") || baseUrl.contains("/v1/chat/completions")) {
            return ensureUrlProtocol(baseUrl);
        }

        if (baseUrl.contains("v1")) {
            return ensureUrlProtocol(baseUrl) + "/chat/completions";
        }
        if (baseUrl.endsWith("#")) {
            return ensureUrlProtocol(baseUrl);
        } else {
            return ensureUrlProtocol(baseUrl) + "/v1/chat/completions";
        }
    }

    /**
     * 确保URL有正确的协议前缀
     */
    private String ensureUrlProtocol(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        // 默认使用https
        return "https://" + url;
    }

    private String buildRequestBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel() != null ? request.getModel() : config.getDefaultModel());
        body.put("messages", request.getMessages());
        body.put("stream", request.isStream());
        body.put("tools", request.getTools());
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        body.put("temperature", request.getTemperature());
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getStreamOptions() != null) {
            body.put("stream_options", request.getStreamOptions());
        }
        if (request.getPresencePenalty() != null) {
            body.put("presence_penalty", request.getPresencePenalty());
        }
        if (request.getResponseFormat() != null) {
            body.put("response_format", request.getResponseFormat());
        }
        if (request.getMaxInputTokens() != null) {
            body.put("max_input_tokens", request.getMaxInputTokens());
        }
        if (request.getVlHighResolutionImages() != null) {
            body.put("vl_high_resolution_images", request.getVlHighResolutionImages());
        }
        if (request.getEnableThinking() != null) {
            body.put("enable_thinking", request.getEnableThinking());
        }
        if (request.getThinkingBudget() != null) {
            body.put("thinking_budget", request.getThinkingBudget());
        }
        if (request.getToolChoice() != null) {
            body.put("tool_choice", request.getToolChoice());
        }
        if (request.getStop() != null) {
            body.put("stop", request.getStop());
        }
        if (request.getParallelToolCalls() != null) {
            body.put("parallel_tool_calls", request.getParallelToolCalls());
        }
        if (request.getEnableSearch() != null) {
            body.put("enable_search", request.getEnableSearch());
        }
        if (request.getFrequencyPenalty() != null) {
            body.put("frequency_penalty", request.getFrequencyPenalty());
        }

        return JsonUtils.encode(body);
    }

    @SuppressWarnings("unchecked")
    Object resolveLocalResourceImageUrls(Object value) throws Exception {
        if (resourceService == null || value == null) {
            return value;
        }

        if (value instanceof Map<?, ?> map) {
            Object imageUrl = map.get("image_url");
            if (imageUrl instanceof Map<?, ?> imageUrlMap) {
                Object url = imageUrlMap.get("url");
                if (url instanceof String urlString) {
                    String dataUrl = resolveLocalResourceDataUrl(urlString);
                    if (dataUrl != null) {
                        ((Map<String, Object>) imageUrlMap).put("url", dataUrl);
                    }
                }
            }

            for (Object child : map.values()) {
                resolveLocalResourceImageUrls(child);
            }
            return value;
        }

        if (value instanceof List<?> list) {
            for (Object item : list) {
                resolveLocalResourceImageUrls(item);
            }
        }

        return value;
    }

    private String resolveLocalResourceDataUrl(String url) throws Exception {
        String resourceName = getResourceNameFromDownloadUrl(url);
        if (resourceName == null) {
            return null;
        }

        Resource resource = resourceService.queryResourceByName(resourceName);
        if (resource == null) {
            throw new ServiceException("400", "Resource image not found: " + resourceName);
        }

        String imageData = resourceName.startsWith("thumbnail_")
            ? resource.getThumbnailData()
            : resource.getResourceData();
        if (!StringUtils.hasText(imageData)) {
            throw new ServiceException("400", "Resource image data is empty: " + resourceName);
        }

        return imageData;
    }

    private String getResourceNameFromDownloadUrl(String url) {
        String path;
        try {
            URI uri = URI.create(url);
            path = uri.getPath();
            if (!StringUtils.hasText(path)) {
                path = url;
            }
        } catch (IllegalArgumentException e) {
            path = url;
        }

        String downloadPath = "/material-center/api/resource/download/";
        int index = path.indexOf(downloadPath);
        if (index < 0) {
            return null;
        }

        String resourceName = path.substring(index + downloadPath.length());
        if (!StringUtils.hasText(resourceName)) {
            return null;
        }

        return URLDecoder.decode(resourceName, StandardCharsets.UTF_8);
    }

    private JsonNode processStandardResponse(HttpRequest.Builder requestBuilder) {
        HttpResponse<String> response = null;
        String code = null;
        String message = null;
        try {
         response = httpClient.send(
             requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
         code = String.valueOf(response.statusCode());
            if (response.statusCode() != 200) {
                String errorBody = response.body();

                // 尝试解析错误JSON
                JsonNode errorNode = JsonUtils.MAPPER.readTree(errorBody);
                message = errorNode.get("error").get("message").asText();
                throw new ServiceException(code, message);
            }
            return JsonUtils.MAPPER.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            throw new ServiceException(code, message);
        }


    }

    private StreamingResponseBody processStreamResponse(HttpRequest.Builder requestBuilder) {
        return outputStream -> {
            HttpResponse<InputStream> response = null;
            try {
                response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream()
                );
            } catch (InterruptedException e) {
                throw new ServiceException("500", e.getMessage());
            }

            log.info("Received AI API response, status code {}", response.statusCode());

            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);

                log.info("errorBody: {}", errorBody);

                writeStreamError(outputStream, response.statusCode(), extractErrorMessage(errorBody));
                return;
            }

            // 正常流处理逻辑
            try (InputStream inputStream = response.body()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            }
        };
    }

    private void writeStreamError(OutputStream outputStream, int statusCode, String message) throws IOException {
        Map<String, Object> errorChunk = Map.of(
            "id", "error",
            "object", "chat.completion.chunk",
            "choices", List.of(Map.of(
                "index", 0,
                "delta", Map.of("content", "AI service error: " + message),
                "finish_reason", "error"
            )),
            "error", Map.of(
                "code", String.valueOf(statusCode),
                "message", message
            )
        );
        outputStream.write(("data: " + JsonUtils.encode(errorChunk) + "\n\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private String extractErrorMessage(String errorBody) {
        try {
            JsonNode errorNode = JsonUtils.MAPPER.readTree(errorBody);
            JsonNode messageNode = errorNode.path("error").path("message");
            if (!messageNode.isMissingNode() && !messageNode.isNull()) {
                return messageNode.asText();
            }
            JsonNode message = errorNode.path("message");
            if (!message.isMissingNode() && !message.isNull()) {
                return message.asText();
            }
        } catch (IOException e) {
            log.warn("Failed to parse AI error body: {}", errorBody, e);
        }

        return StringUtils.hasText(errorBody) ? errorBody : "Unknown AI service error";
    }

    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

    void validateFinalUrl(String finalUrl) {
        URI uri;
        try {
            uri = new URI(finalUrl);
        } catch (URISyntaxException e) {
            throw new ServiceException("400", "Invalid baseUrl format");
        }

        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new ServiceException("400", "Invalid baseUrl: missing host");
        }

        boolean isLoopback = LOOPBACK_HOSTS.contains(host.toLowerCase(Locale.ROOT));

        List<String> allowedHosts = config.getAllowedHosts();

        if (allowedHosts == null || allowedHosts.isEmpty()) {
            if (!config.isAllowAnyHost()) {
                throw new ServiceException("500", "No AI allowed hosts configured");
            }

            enforceHttpsAndIpCheck(uri, host);
            return;
        }

        boolean matched = allowedHosts.stream()
            .anyMatch(allowed -> allowed.equalsIgnoreCase(host));
        if (!matched) {
            throw new ServiceException("400",
                "Host not allowed: " + host + ". Allowed hosts: " + allowedHosts);
        }

        if (isLoopback) {
            return;
        }

        enforceHttpsAndIpCheck(uri, host);
    }

    void enforceHttpsAndIpCheck(URI uri, String host) {
        String scheme = uri.getScheme();
        if (scheme == null || !"https".equalsIgnoreCase(scheme)) {
            throw new ServiceException("400", "Only HTTPS protocol is allowed for custom baseUrl");
        }

        try {
            InetAddress[] addresses = resolveHostAddresses(host);
            boolean hasBlockedAddress = Arrays.stream(addresses).anyMatch(this::isBlockedAddress);
            if (hasBlockedAddress) {
                throw new ServiceException("400", "Internal network addresses are not allowed");
            }
        } catch (UnknownHostException e) {
            throw new ServiceException("400", "Unable to resolve host: " + host);
        }
    }

    InetAddress[] resolveHostAddresses(String host) throws UnknownHostException {
        return InetAddress.getAllByName(host);
    }

    boolean isBlockedAddress(InetAddress address) {
        if (address.isLoopbackAddress()
            || address.isSiteLocalAddress()
            || address.isLinkLocalAddress()
            || address.isAnyLocalAddress()
            || address.isMulticastAddress()) {
            return true;
        }

        if (address instanceof Inet4Address) {
            return isBlockedIpv4((Inet4Address) address);
        }
        if (address instanceof Inet6Address) {
            return isBlockedIpv6((Inet6Address) address);
        }
        return false;
    }

    private boolean isBlockedIpv4(Inet4Address address) {
        byte[] octets = address.getAddress();
        int first = octets[0] & 0xFF;
        int second = octets[1] & 0xFF;
        int third = octets[2] & 0xFF;

        if (first == 0) {
            return true;
        }
        if (first == 100 && second >= 64 && second <= 127) {
            return true;
        }
        if (first == 192 && second == 0 && third == 0) {
            return true;
        }
        if (first == 192 && second == 0 && third == 2) {
            return true;
        }
        if (first == 198 && (second == 18 || second == 19)) {
            return true;
        }
        if (first == 198 && second == 51 && third == 100) {
            return true;
        }
        if (first == 203 && second == 0 && third == 113) {
            return true;
        }
        return first >= 240;
    }

    private boolean isBlockedIpv6(Inet6Address address) {
        byte[] octets = address.getAddress();
        int first = octets[0] & 0xFF;
        int second = octets[1] & 0xFF;

        if ((first & 0xFE) == 0xFC) {
            return true;
        }
        if (first == 0x20 && second == 0x01) {
            int third = octets[2] & 0xFF;
            int fourth = octets[3] & 0xFF;
            if (third == 0x0D && fourth == 0xB8) {
                return true;
            }
        }
        return first == 0xFF;
    }

    private String getApiKey(String encryptApiKey) throws Exception {
        if (!encryptApiKey.startsWith("EKEY_")) {
            return encryptApiKey;
        }

        String sm4Key = aiSecretConfig.getRequiredSm4Key();

        if (encryptApiKey.startsWith("EKEY_")) {
            String encryptBase64ApiKey = encryptApiKey.substring(5);
            return SM4Utils.decryptECB(encryptBase64ApiKey, sm4Key);
        }
        return encryptApiKey;
    }
}
