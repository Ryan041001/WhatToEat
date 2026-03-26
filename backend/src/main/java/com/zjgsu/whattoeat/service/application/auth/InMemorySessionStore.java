package com.zjgsu.whattoeat.service.application.auth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySessionStore {

    private final Map<String, Long> tokenToUserId = new ConcurrentHashMap<>();

    public String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public void save(String token, Long userId) {
        tokenToUserId.put(token, userId);
    }

    public Long getUserId(String token) {
        return tokenToUserId.get(token);
    }

    public void remove(String token) {
        tokenToUserId.remove(token);
    }
}
