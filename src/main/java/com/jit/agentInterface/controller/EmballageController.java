package com.jit.agentInterface.controller;

import com.jit.agentInterface.model.Boite;
import com.jit.agentInterface.model.Chariot;
import com.jit.agentInterface.service.EmballageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emballage")
@Tag(name = "Emballage")
public class EmballageController {

    private final EmballageService emballageService;

    public EmballageController(EmballageService emballageService) {
        this.emballageService = emballageService;
    }

    @PostMapping("/chariots")
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Agent_industrielle.name(), T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Ajouter un chariot vide")
    public ResponseEntity<Map<String, Object>> ajouterChariot() {
        long count = emballageService.addChariot();
        return ResponseEntity.ok(Map.of("chariots", count));
    }

    @DeleteMapping("/chariots/{index}")
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Agent_industrielle.name(), T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Supprimer un chariot par index")
    public ResponseEntity<Map<String, Object>> supprimerChariot(@PathVariable @Min(0) int index) {
        long count = emballageService.removeChariotByIndex(index);
        return ResponseEntity.ok(Map.of("chariots", count));
    }

    public record AjouterBoiteRequest(@NotNull Boite boite) {}

    @PostMapping("/chariots/{index}/boites")
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Agent_industrielle.name(), T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Ajouter une boite a un chariot")
    public ResponseEntity<Map<String, Object>> ajouterBoite(@PathVariable @Min(0) int index, @RequestBody AjouterBoiteRequest req) {
        int totalBoites = emballageService.addBoiteToChariot(index, req.boite);
        return ResponseEntity.ok(Map.of("boitesEmballees", totalBoites));
    }

    @DeleteMapping("/chariots/{index}/boites/{indexBoite}")
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Agent_industrielle.name(), T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Retirer une boite d'un chariot")
    public ResponseEntity<Map<String, Object>> retirerBoite(@PathVariable @Min(0) int index, @PathVariable @Min(0) int indexBoite) {
        int totalBoites = emballageService.removeBoiteFromChariot(index, indexBoite);
        return ResponseEntity.ok(Map.of("boitesEmballees", totalBoites));
    }

    @GetMapping("/chariots")
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Agent_industrielle.name(), T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Lister les chariots d'emballage (ordre croissant d'id)")
    public ResponseEntity<List<Chariot>> listerChariots() {
        return ResponseEntity.ok(emballageService.allChariotsOrdered());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Agent_industrielle.name(), T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Résumé de l'état d'emballage (nombre chariots, boites emballées)")
    public ResponseEntity<Map<String, Object>> resume() {
        return ResponseEntity.ok(emballageService.summary());
    }
}
