package com.jit.agentInterface.controller;

import com.jit.agentInterface.enums.Type;
import com.jit.agentInterface.service.StockBruteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-brute")
@Tag(name = "Stock brut")
public class StockBruteController {

    private final StockBruteService stockService;

    public StockBruteController(StockBruteService stockService) {
        this.stockService = stockService;
    }

    public record MouvementRequest(@NotNull Type type, @NotNull BigDecimal quantite) {}

    @PostMapping("/augmenter")
    @Transactional
    @PreAuthorize("hasRole(T(com.jit.agentInterface.enums.Role).Agent_industrielle.name()) or hasRole(T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Augmenter la quantite du stock brut pour un type")
    public ResponseEntity<Map<String, Object>> augmenter(@RequestBody MouvementRequest req) {
        var q = stockService.augmenter(req.type, req.quantite);
        return ResponseEntity.ok(Map.of("type", req.type, "quantite", q));
    }

    @PostMapping("/diminuer")
    @Transactional
    @PreAuthorize("hasRole(T(com.jit.agentInterface.enums.Role).Agent_industrielle.name()) or hasRole(T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Diminuer la quantite du stock brut pour un type")
    public ResponseEntity<Map<String, Object>> diminuer(@RequestBody MouvementRequest req) {
        var q = stockService.diminuer(req.type, req.quantite);
        return ResponseEntity.ok(Map.of("type", req.type, "quantite", q));
    }

    @GetMapping("/quantite")
    @PreAuthorize("hasRole(T(com.jit.agentInterface.enums.Role).Agent_industrielle.name()) or hasRole(T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Consulter la quantite pour un type de stock brut")
    public ResponseEntity<Map<String, Object>> quantite(@Parameter(description = "Type de produit") @RequestParam Type type) {
        var q = stockService.quantite(type);
        return ResponseEntity.ok(Map.of("type", type, "quantite", q));
    }
}
