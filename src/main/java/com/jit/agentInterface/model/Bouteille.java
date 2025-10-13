package com.jit.agentInterface.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;

@Entity
public class Bouteille extends Produit {
    @Embedded
    private Litrage litrage;

    public Litrage getLitrage() { return litrage; }
    public void setLitrage(Litrage litrage) { this.litrage = litrage; }
}
