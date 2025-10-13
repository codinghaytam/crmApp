package com.jit.agentInterface.controller;

import com.jit.agentInterface.enums.Type;
import com.jit.agentInterface.model.Bouteille;
import com.jit.agentInterface.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/industrial")
@Tag(name = "Agent industriel")
public class IndustrialController {

    private final ProductService productService;

    public IndustrialController(ProductService productService) {
        this.productService = productService;
    }

    public record BouteilleRequest(@NotNull Type type, double litrage, double prix) {}

    @PostMapping("/bouteilles")
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Agent_industrielle.name(), T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Fabriquer une bouteille: consomme le stock brut du type choisi")
    public ResponseEntity<Bouteille> fabriquerBouteille(@RequestBody BouteilleRequest req) {
        return ResponseEntity.ok(productService.createBouteille(req.type, req.litrage, req.prix));
    }
}
