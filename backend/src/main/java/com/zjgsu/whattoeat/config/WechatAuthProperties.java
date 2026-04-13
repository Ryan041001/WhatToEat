package com.zjgsu.whattoeat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wechat.auth")
public record WechatAuthProperties(
        String appId,
        String appSecret,
        String baseUrl,
        boolean mockLoginEnabled) {
}
