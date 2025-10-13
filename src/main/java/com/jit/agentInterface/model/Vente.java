package com.jit.agentInterface.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jit.agentInterface.enums.VenteType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ventes")
public class Vente {
    @Id
    private String id;

    private Instant dateVente;

    @Enumerated(EnumType.STRING)
    private VenteType type; // AU_VENDEUR ou DU_VENDEUR

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendeur_id")
    private User vendeur;

    private double montantTotal;

    @Lob
    @Column(name = "lignes_json")
    private String lignesJson; // stockage persistant

    @Transient
    private List<Ligne> lignes = new ArrayList<>();

    public static class Ligne {
        private String typeLigne; // BOITE or BOUTEILLE (pour AU_VENDEUR) ; BOUTEILLE pour DU_VENDEUR
        private String produitId; // id du produit vendu (Boite ou Bouteille)
        private int quantite;
        private double prixUnitaire;

        public Ligne() {}
        public Ligne(String typeLigne, String produitId, int quantite, double prixUnitaire) {
            this.typeLigne = typeLigne;
            this.produitId = produitId;
            this.quantite = quantite;
            this.prixUnitaire = prixUnitaire;
        }
        public String getTypeLigne() { return typeLigne; }
        public void setTypeLigne(String typeLigne) { this.typeLigne = typeLigne; }
        public String getProduitId() { return produitId; }
        public void setProduitId(String produitId) { this.produitId = produitId; }
        public int getQuantite() { return quantite; }
        public void setQuantite(int quantite) { this.quantite = quantite; }
        public double getPrixUnitaire() { return prixUnitaire; }
        public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
        public double getSousTotal() { return prixUnitaire * quantite; }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (dateVente == null) dateVente = Instant.now();
        if (lignes != null) {
            montantTotal = lignes.stream().mapToDouble(Ligne::getSousTotal).sum();
            try { lignesJson = MAPPER.writeValueAsString(lignes); } catch (Exception ignored) {}
        }
    }

    @PostLoad
    public void postLoad() {
        if (lignesJson != null && !lignesJson.isBlank()) {
            try { lignes = MAPPER.readValue(lignesJson, new TypeReference<List<Ligne>>(){}); } catch (Exception ignored) {}
        }
    }

    // Getters / setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Instant getDateVente() { return dateVente; }
    public void setDateVente(Instant dateVente) { this.dateVente = dateVente; }

    public VenteType getType() { return type; }
    public void setType(VenteType type) { this.type = type; }

    public User getVendeur() { return vendeur; }
    public void setVendeur(User vendeur) { this.vendeur = vendeur; }

    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }

    public List<Ligne> getLignes() { return lignes; }
    public void setLignes(List<Ligne> lignes) { this.lignes = lignes; }

    // Aide OpenAPI
    public Long getVendeurId() { return vendeur != null ? vendeur.getId() : null; }
}
