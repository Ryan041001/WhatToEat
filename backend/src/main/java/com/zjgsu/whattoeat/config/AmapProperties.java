package com.zjgsu.whattoeat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amap")
public record AmapProperties(String key, String baseUrl, int timeoutSeconds) {
}
