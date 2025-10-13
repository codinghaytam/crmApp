package com.jit.agentInterface.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.List;

@Entity
public class Chariot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    @JoinTable(name = "chariot_boites",
            joinColumns = @JoinColumn(name = "chariot_id"),
            inverseJoinColumns = @JoinColumn(name = "boite_id"))
    List<Boite> boites;

    public Long getId() { return id; }

    public List<Boite> getBoites() { return boites; }
    public void setBoites(List<Boite> boites) { this.boites = boites; }
}
