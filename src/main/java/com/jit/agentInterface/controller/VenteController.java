package com.jit.agentInterface.controller;

import com.jit.agentInterface.enums.VenteType;
import com.jit.agentInterface.model.Vente;
import com.jit.agentInterface.service.VenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ventes")
@Tag(name = "Ventes")
@Validated
public class VenteController {

    private final VenteService venteService;

    public VenteController(VenteService venteService) {
        this.venteService = venteService;
    }

    public record LigneRequest(@NotBlank String typeLigne, @NotBlank String produitId, @Min(1) int quantite, @Min(0) double prixUnitaire) {}
    public record VenteRequest(@NotNull VenteType type, @NotBlank String vendeurId, @NotEmpty List<@Valid LigneRequest> lignes) {}

    @PostMapping
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Admin.name(), T(com.jit.agentInterface.enums.Role).Agent_commercial.name(), T(com.jit.agentInterface.enums.Role).Vendeur.name())")
    @Operation(summary = "Créer une vente unifiée", description = "type=AU_VENDEUR (lignes: BOITE/BOUTEILLE) ou type=DU_VENDEUR (lignes: BOUTEILLE uniquement)")
    public ResponseEntity<Vente> creer(@Valid @RequestBody VenteRequest req, Authentication auth) {
        List<VenteService.LigneInput> inputs = req.lignes.stream()
                .map(l -> new VenteService.LigneInput(l.typeLigne, l.produitId, l.quantite, l.prixUnitaire))
                .toList();
        Vente saved = venteService.createVente(req.type, req.vendeurId, inputs, auth);
        return ResponseEntity.status(201).body(saved);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Admin.name(), T(com.jit.agentInterface.enums.Role).Agent_commercial.name(), T(com.jit.agentInterface.enums.Role).Vendeur.name())")
    @Operation(summary = "Lister les ventes", description = "Filtrer par type, date, vendeurId")
    public ResponseEntity<List<Vente>> lister(
            @RequestParam(required = false) VenteType type,
            @Parameter(description = "Date début") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @Parameter(description = "Date fin") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @Parameter(description = "Identifiant vendeur") @RequestParam(required = false) String vendeurId
    ) {
        return ResponseEntity.ok(venteService.listVentes(type, dateDebut, dateFin, vendeurId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Admin.name(), T(com.jit.agentInterface.enums.Role).Agent_commercial.name(), T(com.jit.agentInterface.enums.Role).Vendeur.name())")
    @Operation(summary = "Obtenir une vente par ID")
    public ResponseEntity<Vente> obtenir(@PathVariable String id) {
        return ResponseEntity.of(venteService.getVente(id));
    }
}
