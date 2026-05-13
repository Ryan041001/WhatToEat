package com.zjgsu.whattoeat.service.application.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemorySessionStoreTest {

    @Test
    void getUserIdShouldReturnNullAndRemoveSessionWhenTokenExpired() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-06T00:00:00Z"));
        InMemorySessionStore store = new InMemorySessionStore(clock, Duration.ofMinutes(30));

        store.save("token-1", 42L);
        assertEquals(42L, store.getUserId("token-1"));

        clock.advance(Duration.ofMinutes(30));

        assertNull(store.getUserId("token-1"));
        assertNull(store.getUserId("token-1"));
    }

    @Test
    void constructorShouldRejectNonPositiveTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-06T00:00:00Z"));

        assertThrows(IllegalArgumentException.class, () -> new InMemorySessionStore(clock, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new InMemorySessionStore(clock, Duration.ofMinutes(-1)));
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
