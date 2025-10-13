package com.jit.agentInterface.model;

import com.jit.agentInterface.enums.Type;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import java.util.List;
import java.util.Objects;

@MappedSuperclass
public abstract class Produit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    Type type;

    double Prix;

    @Transient
    List<Vente> ventes;

    // Accesseurs de base
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public double getPrix() { return Prix; }
    public void setPrix(double prix) { this.Prix = prix; }

    public List<Vente> getVentes() { return ventes; }
    public void setVentes(List<Vente> ventes) { this.ventes = ventes; }

    // Méthodes métier minimales communes
    public double prixUnitaire() { return getPrix(); }

    public boolean estDeType(Type t) { return Objects.equals(this.type, t); }
}
