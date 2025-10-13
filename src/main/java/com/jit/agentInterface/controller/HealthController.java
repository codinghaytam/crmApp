package com.jit.agentInterface.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(summary = "Vérifier l'état de l'application")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}

