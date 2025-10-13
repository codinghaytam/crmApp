package com.jit.agentInterface.service;

import com.jit.agentInterface.enums.Type;
import com.jit.agentInterface.model.StockBrute;
import com.jit.agentInterface.repository.ChariotRepository;
import com.jit.agentInterface.repository.StockBruteRepository;
import com.jit.agentInterface.repository.VenteRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminService {

    private final StockBruteRepository stockRepo;
    private final VenteRepository venteRepo;
    private final ChariotRepository chariotRepository;

    public AdminService(StockBruteRepository stockRepo, VenteRepository venteRepo, ChariotRepository chariotRepository) {
        this.stockRepo = stockRepo;
        this.venteRepo = venteRepo;
        this.chariotRepository = chariotRepository;
    }

    public Map<String, Object> stats() {
        Map<String, Object> m = new HashMap<>();
        Map<String, Object> stock = new HashMap<>();
        StockBrute sb = stockRepo.findAll().stream().findFirst().orElse(null);
        for (Type t : Type.values()) {
            stock.put(t.name(), sb == null ? java.math.BigDecimal.ZERO : sb.quantite(t));
        }
        m.put("stockBrut", stock);
        int totalBoites = chariotRepository.findAll().stream().mapToInt(c -> c.getBoites() == null ? 0 : c.getBoites().size()).sum();
        m.put("boitesEmballees", totalBoites);
        long ventesTotal = venteRepo.count();
        m.put("ventesCount", ventesTotal);
        return m;
    }
}

