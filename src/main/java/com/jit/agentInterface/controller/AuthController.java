package com.jit.agentInterface.controller;

import com.jit.agentInterface.enums.Role;
import com.jit.agentInterface.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String token, Instant expiresAt, List<String> roles) {}

    public record SignupRequest(@NotBlank String nom, @NotBlank String prenom, @NotBlank String email, @NotBlank String password, Role role) {}
    public record SignupResponse(Long userId, String username, String token, Instant expiresAt, List<String> roles) {}

    @PostMapping("/login")
    @Operation(summary = "Connexion et émission d'un JWT (username = email)")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        var result = authService.login(req.username, req.password);
        return ResponseEntity.ok(new LoginResponse(result.token(), result.expiresAt(), result.roles()));
    }

    @PostMapping("/signup")
    @Operation(summary = "Créer un utilisateur et émettre un JWT", description = "Seul Admin peut créer un rôle non Vendeur.")
    public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest req, Authentication auth) {
        var s = authService.signup(req.nom, req.prenom, req.email, req.password, req.role, auth);
        return ResponseEntity.status(201).body(new SignupResponse(s.userId(), s.username(), s.token(), s.expiresAt(), s.roles()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion (JWT blacklisté sur le serveur jusqu'à expiration)")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) token = authorization.substring(7);
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }
}
