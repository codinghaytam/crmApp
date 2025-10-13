package com.jit.agentInterface.controller;

import com.jit.agentInterface.enums.Type;
import com.jit.agentInterface.model.Boite;
import com.jit.agentInterface.model.Bouteille;
import com.jit.agentInterface.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commercial")
@Tag(name = "Commercial")
public class CommercialController {

    private final ProductService productService;

    public CommercialController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/types")
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Agent_commercial.name(), T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Lister les types de produit disponibles")
    public ResponseEntity<List<String>> types() {
        return ResponseEntity.ok(java.util.Arrays.stream(Type.values()).map(Enum::name).toList());
    }

    @GetMapping("/litrages")
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Agent_commercial.name(), T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Lister les litrages autorisés")
    public ResponseEntity<double[]> litrages() {
        return ResponseEntity.ok(new double[]{1,0.5,2,5});
    }

    public record BouteilleRequest(@NotNull Type type, double litrage, double prix) {}

    @PostMapping("/bouteilles")
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Agent_commercial.name(), T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Créer une bouteille")
    public ResponseEntity<Bouteille> creerBouteille(@RequestBody BouteilleRequest req) {
        return ResponseEntity.ok(productService.createBouteille(req.type, req.litrage, req.prix));
    }

    public record BoiteRequest(@NotNull List<Bouteille> bouteilles, int quantite, double prix) {}

    @PostMapping("/boites")
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Agent_commercial.name(), T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Créer une boite")
    public ResponseEntity<Boite> creerBoite(@RequestBody BoiteRequest req) {
        return ResponseEntity.ok(productService.createBoite(req.bouteilles, req.quantite, req.prix));
    }
}
