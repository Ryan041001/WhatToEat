package com.zjgsu.whattoeat.integration.wechat;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.config.WechatAuthProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

@Component
public class WechatAuthClient implements WechatAuthGateway {

    private static final String CODE_2_SESSION_PATH = "/sns/jscode2session";
    private static final Set<Integer> INVALID_CODE_ERROR_CODES = Set.of(40029, 40163, 40226);

    private final RestClient restClient;
    private final WechatAuthProperties properties;

    public WechatAuthClient(RestClient.Builder builder, WechatAuthProperties properties) {
        this.properties = properties;
        this.restClient = builder.baseUrl(resolveBaseUrl(properties.baseUrl())).build();
    }

    @Override
    public WechatSession exchangeCode(String code) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(CODE_2_SESSION_PATH)
                            .queryParam("appid", getRequiredProperty("appId"))
                            .queryParam("secret", getRequiredProperty("appSecret"))
                            .queryParam("js_code", code)
                            .queryParam("grant_type", "authorization_code")
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (body == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }

            Integer errcode = parseInteger(body.get("errcode"));
            if (errcode != null && errcode != 0) {
                if (INVALID_CODE_ERROR_CODES.contains(errcode)) {
                    throw new BusinessException(ErrorCode.LOGIN_CODE_INVALID);
                }
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }

            String openid = readRequiredText(body, "openid");
            String sessionKey = readRequiredText(body, "session_key");
            String unionid = readOptionalText(body, "unionid");
            return new WechatSession(openid, sessionKey, unionid);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private String getRequiredProperty(String name) {
        String value = switch (name) {
            case "appId" -> properties.appId();
            case "appSecret" -> properties.appSecret();
            default -> null;
        };
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return value;
    }

    private static String resolveBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.weixin.qq.com";
        }
        return baseUrl;
    }

    private String readRequiredText(Map<String, Object> body, String key) {
        String value = readOptionalText(body, key);
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return value;
    }

    private String readOptionalText(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private Integer parseInteger(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
