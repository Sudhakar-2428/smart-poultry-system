package com.poultry.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@Tag(name = "Root Welcome", description = "Root status endpoint for system health and service verification")
public class RootWelcomeController {

    @GetMapping("/")
    @Operation(summary = "Root API status", description = "Returns system status when visiting the base application domain")
    public ResponseEntity<Map<String, Object>> rootStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "smart-poultry-backend",
                "message", "Smart Poultry Management System API is running successfully.",
                "version", "1.0.0",
                "timestamp", Instant.now().toString(),
                "healthEndpoint", "/api/v1/health",
                "swaggerDocumentation", "/swagger-ui/index.html"
        ));
    }
}
