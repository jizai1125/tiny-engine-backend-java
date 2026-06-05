package com.tinyengine.it.service.app.impl.v1;

import com.tinyengine.it.common.exception.ServiceException;
import com.tinyengine.it.config.OpenAIConfig;
import com.tinyengine.it.model.entity.Resource;
import com.tinyengine.it.service.material.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiChatV1ServiceImplTest {
    private TestAiChatV1ServiceImpl service;
    private OpenAIConfig config;

    @BeforeEach
    void setUp() throws Exception {
        config = new OpenAIConfig();
        service = new TestAiChatV1ServiceImpl(config);
        service.stubHost("api.openai.com", "8.8.8.8");
        service.stubHost("api.deepseek.com", "1.1.1.1");
        service.stubHost("example.com", "93.184.216.34");
    }

    // === 无白名单模式（默认 fail-closed）===

    @Test
    void shouldRejectWhenNoAllowedHostsConfigured() {
        ServiceException exception = assertThrows(ServiceException.class, () ->
            service.validateFinalUrl("https://api.openai.com/v1/chat/completions"));
        assertEquals("No AI allowed hosts configured", exception.getMessage());
    }

    @Test
    void shouldAllowPublicHttpsUrlWhenAllowAnyHostEnabled() {
        config.setAllowAnyHost(true);
        assertDoesNotThrow(() ->
            service.validateFinalUrl("https://api.openai.com/v1/chat/completions"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://127.0.0.1:8080/v1/chat/completions",
        "http://localhost:11434/v1/chat/completions",
        "https://192.168.1.1/v1/chat/completions",
        "https://10.0.0.1/v1/chat/completions",
        "https://169.254.169.254/latest/meta-data/"
    })
    void shouldRejectInternalAddresses(String url) {
        config.setAllowAnyHost(true);
        assertThrows(ServiceException.class, () ->
            service.validateFinalUrl(url));
    }

    @Test
    void shouldRejectHttpForPublicHost() {
        config.setAllowAnyHost(true);
        assertThrows(ServiceException.class, () ->
            service.validateFinalUrl("http://api.openai.com/v1/chat/completions"));
    }

    @Test
    void shouldRejectInvalidUrl() {
        config.setAllowAnyHost(true);
        assertThrows(ServiceException.class, () ->
            service.validateFinalUrl("not-a-valid-url"));
    }

    // === 白名单模式 ===

    @Test
    void shouldAllowWhitelistedHost() {
        config.setAllowedHosts(List.of("api.deepseek.com", "api.openai.com"));
        assertDoesNotThrow(() ->
            service.validateFinalUrl("https://api.deepseek.com/v1/chat/completions"));
    }

    @Test
    void shouldRejectNonWhitelistedHost() {
        config.setAllowedHosts(List.of("api.deepseek.com"));
        assertThrows(ServiceException.class, () ->
            service.validateFinalUrl("https://api.openai.com/v1/chat/completions"));
    }

    @Test
    void shouldAllowWhitelistedLocalhostWithHttp() {
        config.setAllowedHosts(List.of("localhost", "127.0.0.1"));
        assertDoesNotThrow(() ->
            service.validateFinalUrl("http://localhost:11434/v1/chat/completions"));
    }

    @Test
    void shouldRejectHttpForWhitelistedExternalHost() {
        config.setAllowedHosts(List.of("api.deepseek.com"));
        assertThrows(ServiceException.class, () ->
            service.validateFinalUrl("http://api.deepseek.com/v1/chat/completions"));
    }

    @Test
    void shouldRejectCarrierGradeNatAddress() {
        config.setAllowAnyHost(true);
        assertThrows(ServiceException.class, () ->
            service.validateFinalUrl("https://100.64.0.1/v1/chat/completions"));
    }

    @Test
    void shouldRejectBenchmarkingAddress() {
        config.setAllowAnyHost(true);
        assertThrows(ServiceException.class, () ->
            service.validateFinalUrl("https://198.18.0.1/v1/chat/completions"));
    }

    @Test
    void shouldRejectIpv6UniqueLocalAddress() {
        config.setAllowAnyHost(true);
        assertThrows(ServiceException.class, () ->
            service.validateFinalUrl("https://[fc00::1]/v1/chat/completions"));
    }

    @Test
    void shouldConvertLocalResourceImageUrlToDataUrl() throws Exception {
        ResourceService resourceService = mock(ResourceService.class);
        Resource resource = new Resource();
        resource.setResourceData("data:image/png;base64,abc123");
        when(resourceService.queryResourceByName("image123.png")).thenReturn(resource);

        TestAiChatV1ServiceImpl serviceWithResource = new TestAiChatV1ServiceImpl(config, resourceService);
        Map<String, Object> imageUrl = new HashMap<>();
        imageUrl.put("url", "http://localhost:9090/material-center/api/resource/download/image123.png");
        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");
        imageContent.put("image_url", imageUrl);
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", List.of(imageContent));

        serviceWithResource.resolveLocalResourceImageUrls(List.of(message));

        assertEquals("data:image/png;base64,abc123", imageUrl.get("url"));
    }

    private static final class TestAiChatV1ServiceImpl extends AiChatV1ServiceImpl {
        private final Map<String, InetAddress[]> resolvedHosts = new HashMap<>();

        private TestAiChatV1ServiceImpl(OpenAIConfig config) {
            super(config);
        }

        private TestAiChatV1ServiceImpl(OpenAIConfig config, ResourceService resourceService) {
            super(config, null, null, resourceService);
        }

        private void stubHost(String host, String... addresses) throws UnknownHostException {
            InetAddress[] resolved = new InetAddress[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                resolved[i] = InetAddress.getByName(addresses[i]);
            }
            resolvedHosts.put(host, resolved);
        }

        @Override
        InetAddress[] resolveHostAddresses(String host) throws UnknownHostException {
            InetAddress[] resolved = resolvedHosts.get(host);
            if (resolved != null) {
                return resolved;
            }
            return super.resolveHostAddresses(host);
        }
    }
}
