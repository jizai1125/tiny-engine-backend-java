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

package com.tinyengine.it.config;

import com.tinyengine.it.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Base64;

@Component
public class AiSecretConfig {
    private final String sm4Key;

    public AiSecretConfig(
        @Value("${ai.sm4-key:}") String configuredSm4Key,
        @Value("${AI_SM4_KEY:}") String envSm4Key,
        @Value("${SM4KEY:}") String legacyEnvSm4Key
    ) {
        this.sm4Key = firstNonBlank(configuredSm4Key, envSm4Key, legacyEnvSm4Key);
    }

    public String getRequiredSm4Key() {
        if (!StringUtils.hasText(sm4Key)) {
            throw new ServiceException("500", "AI SM4 key is not configured");
        }

        String normalized = sm4Key.trim();
        try {
            byte[] decoded = Base64.getDecoder().decode(normalized);
            if (decoded.length != 16) {
                throw new ServiceException("500", "AI SM4 key is invalid: expected Base64-encoded 16-byte key");
            }
            return normalized;
        } catch (IllegalArgumentException e) {
            throw new ServiceException("500", "AI SM4 key is invalid: expected Base64-encoded 16-byte key");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
