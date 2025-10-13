package com.jit.agentInterface.service;

import com.jit.agentInterface.enums.Role;
import com.jit.agentInterface.model.User;
import com.jit.agentInterface.repository.UserRepository;
import com.jit.agentInterface.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AuthService {

    public record AuthResult(String token, Instant expiresAt, List<String> roles) {}
    public record SignupResult(Long userId, String username, String token, Instant expiresAt, List<String> roles) {}

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenBlacklistService tokenBlacklistService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public AuthResult login(String username, String password) {
        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        SecurityContextHolder.getContext().setAuthentication(auth);
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .map(s -> s.startsWith("ROLE_") ? s.substring(5) : s).toList();
        String token = jwtUtil.generateToken(username, null, roles);
        Instant expires = Instant.now().plus(15, ChronoUnit.MINUTES);
        return new AuthResult(token, expires, roles);
    }

    public SignupResult signup(String nom, String prenom, String email, String rawPassword, Role requestedRole, Authentication caller) {
        userRepository.findByEmail(email).ifPresent(u -> { throw new ServiceException(HttpStatus.CONFLICT, "Email déjà utilisé"); });

        // Determine role: default Vendeur. Allow non-Vendeur if caller is Admin OR if no users exist yet (bootstrap)
        Role role = Role.Vendeur;
        boolean bootstrap = userRepository.count() == 0;
        boolean callerIsAdmin = caller != null && caller.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.Admin.name()));
        if (requestedRole != null) {
            if (requestedRole == Role.Vendeur) {
                role = Role.Vendeur;
            } else if (callerIsAdmin || bootstrap) {
                role = requestedRole;
            } else {
                throw new ServiceException(HttpStatus.FORBIDDEN, "Seul un Admin peut créer un utilisateur non Vendeur");
            }
        }

        User u = new User();
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        User saved = userRepository.save(u);
        // Issue a token for convenience
        AuthResult auth = login(email, rawPassword);
        return new SignupResult(saved.getId(), email, auth.token(), auth.expiresAt(), auth.roles());
    }

    public void logout(String token) {
        if (token == null || token.isBlank()) return;
        if (!jwtUtil.isTokenValid(token)) return;
        Instant exp = jwtUtil.getExpiration(token);
        tokenBlacklistService.revoke(token, exp);
        SecurityContextHolder.clearContext();
    }
}
