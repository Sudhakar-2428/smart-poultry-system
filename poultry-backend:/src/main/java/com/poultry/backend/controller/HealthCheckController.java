package com.poultry.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping
@Tag(name = "Health Check", description = "Cloud readiness and liveness health check endpoint")
public class HealthCheckController {

    @GetMapping({"/health", "/api/v1/health"})
    @Operation(summary = "System health status check", description = "Unauthenticated health ping endpoint for cloud load balancers and deployment probes")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "poultry-backend",
                "timestamp", Instant.now().toString()
        ));
    }
}
