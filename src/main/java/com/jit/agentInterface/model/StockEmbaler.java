package com.jit.agentInterface.model;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class StockEmbaler {
    private List<Chariot> chariots;

    public List<Chariot> getChariots() { return chariots == null ? Collections.emptyList() : chariots; }
    public void setChariots(List<Chariot> chariots) { this.chariots = chariots; }

    public int compterBoites() {
        return getChariots().stream().mapToInt(c -> c.getBoites() == null ? 0 : c.getBoites().size()).sum();
    }

    // Métier
    public void ajouterChariot() {
        if (this.chariots == null) this.chariots = new ArrayList<>();
        this.chariots.add(new Chariot());
    }

    public void supprimerChariot(int index) {
        if (this.chariots == null || index < 0 || index >= this.chariots.size())
            throw new IllegalArgumentException("chariot introuvable");
        this.chariots.remove(index);
    }

    public void ajouterBoiteAuChariot(int indexChariot, Boite boite) {
        if (boite == null) throw new IllegalArgumentException("boite null");
        if (this.chariots == null || indexChariot < 0 || indexChariot >= this.chariots.size())
            throw new IllegalArgumentException("chariot introuvable");
        Chariot c = this.chariots.get(indexChariot);
        if (c.getBoites() == null) c.setBoites(new ArrayList<>());
        c.getBoites().add(boite);
    }

    public void retirerBoiteDuChariot(int indexChariot, int indexBoite) {
        if (this.chariots == null || indexChariot < 0 || indexChariot >= this.chariots.size())
            throw new IllegalArgumentException("chariot introuvable");
        Chariot c = this.chariots.get(indexChariot);
        if (c.getBoites() == null || indexBoite < 0 || indexBoite >= c.getBoites().size())
            throw new IllegalArgumentException("boite introuvable");
        c.getBoites().remove(indexBoite);
    }
}
