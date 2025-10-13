package com.jit.agentInterface.controller;

import com.jit.agentInterface.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) { this.adminService = adminService; }

    @GetMapping("/whoami")
    @PreAuthorize("hasRole(T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Informations sur l'utilisateur courant")
    public Map<String, Object> whoami(Authentication auth) {
        return Map.of(
                "username", auth.getName(),
                "roles", auth.getAuthorities()
        );
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole(T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Statistiques globales")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(adminService.stats());
    }
}
