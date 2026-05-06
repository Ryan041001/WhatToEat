package com.zjgsu.whattoeat.service.application.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySessionStore {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration sessionTtl;

    @Autowired
    public InMemorySessionStore(@Value("${whattoeat.auth.session-ttl-minutes:120}") long sessionTtlMinutes) {
        this(Clock.systemUTC(), Duration.ofMinutes(sessionTtlMinutes));
    }

    InMemorySessionStore(Clock clock, Duration sessionTtl) {
        if (sessionTtl == null || sessionTtl.isZero() || sessionTtl.isNegative()) {
            throw new IllegalArgumentException("sessionTtl must be positive");
        }
        this.clock = clock;
        this.sessionTtl = sessionTtl;
    }

    public String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public void save(String token, Long userId) {
        sessions.put(token, new Session(userId, Instant.now(clock).plus(sessionTtl)));
    }

    public Long getUserId(String token) {
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.isExpired(Instant.now(clock))) {
            sessions.remove(token);
            return null;
        }
        return session.userId();
    }

    public void remove(String token) {
        sessions.remove(token);
    }

    private record Session(Long userId, Instant expiresAt) {
        private boolean isExpired(Instant now) {
            return !now.isBefore(expiresAt);
        }
    }
}
