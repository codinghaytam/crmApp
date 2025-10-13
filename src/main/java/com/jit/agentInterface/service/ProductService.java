package com.jit.agentInterface.service;

import com.jit.agentInterface.enums.Type;
import com.jit.agentInterface.model.Boite;
import com.jit.agentInterface.model.Bouteille;
import com.jit.agentInterface.model.ProduitFactory;
import com.jit.agentInterface.repository.BoiteRepository;
import com.jit.agentInterface.repository.BouteilleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProduitFactory produitFactory;
    private final BouteilleRepository bouteilleRepository;
    private final BoiteRepository boiteRepository;

    public ProductService(ProduitFactory produitFactory,
                          BouteilleRepository bouteilleRepository,
                          BoiteRepository boiteRepository) {
        this.produitFactory = produitFactory;
        this.bouteilleRepository = bouteilleRepository;
        this.boiteRepository = boiteRepository;
    }

    public Bouteille createBouteille(Type type, double litrage, double prix) {
        Bouteille created = produitFactory.createBouteille(type, litrage, prix);
        return bouteilleRepository.save(created);
    }

    public Boite createBoite(List<Bouteille> bouteilles, int quantite, double prix) {
        // Ensure all bottles are managed
        List<Bouteille> managed = new ArrayList<>();
        for (Bouteille b : bouteilles) {
            if (b.getId() == null) {
                managed.add(bouteilleRepository.save(b));
            } else {
                managed.add(bouteilleRepository.findById(b.getId()).orElseGet(() -> bouteilleRepository.save(b)));
            }
        }
        Boite boite = produitFactory.createBoite(managed, quantite, prix);
        return boiteRepository.save(boite);
    }
}

