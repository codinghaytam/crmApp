package com.jit.agentInterface.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.List;

@Entity
public class Boite extends Produit {
    private int quantite;

    @ManyToMany
    @JoinTable(name = "boite_bouteilles",
            joinColumns = @JoinColumn(name = "boite_id"),
            inverseJoinColumns = @JoinColumn(name = "bouteille_id"))
    private List<Bouteille> bouteilles;

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = Math.max(0, quantite); }

    public List<Bouteille> getBouteilles() { return bouteilles; }
    public void setBouteilles(List<Bouteille> bouteilles) { this.bouteilles = bouteilles; }

    // Métier
    public void augmenterQuantite(int qte) {
        if (qte < 0) throw new IllegalArgumentException("quantite negative");
        this.quantite += qte;
    }

    public void diminuerQuantite(int qte) {
        if (qte < 0) throw new IllegalArgumentException("quantite negative");
        if (qte > this.quantite) throw new IllegalArgumentException("stock insuffisant");
        this.quantite -= qte;
    }
}
