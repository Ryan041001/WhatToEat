package com.zjgsu.whattoeat.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
public class HealthController {

    private final String version;

    public HealthController(@Value("${spring.application.version:0.0.1-SNAPSHOT}") String version) {
        this.version = version;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("healthy", OffsetDateTime.now().toString(), version);
    }

    public record HealthResponse(String status, String timestamp, String version) {
    }
}
