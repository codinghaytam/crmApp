package com.jit.agentInterface.model;

import com.jit.agentInterface.enums.Type;
import com.jit.agentInterface.enums.EventType;
import com.jit.agentInterface.repository.StockBruteRepository;
import com.jit.agentInterface.service.WebhookDispatcherService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProduitFactory {

    private final StockBruteRepository stockRepo;
    private final WebhookDispatcherService webhookDispatcher;

    public ProduitFactory(StockBruteRepository stockRepo, WebhookDispatcherService webhookDispatcher) {
        this.stockRepo = stockRepo;
        this.webhookDispatcher = webhookDispatcher;
    }

    public Bouteille createBouteille(Type type, double litrageValeur, double prix) {
        // consume stock first (1 bottle consumes 'litrageValeur' from the matching container)
        StockBrute sb = stockRepo.findAll().stream().findFirst().orElseGet(() -> stockRepo.save(new StockBrute()));
        sb.diminuerQuantite(type, BigDecimal.valueOf(litrageValeur));
        stockRepo.save(sb);
        // publish stock change event
        webhookDispatcher.publish(EventType.STOCK_CHANGED, java.util.Map.of(
                "type", type.name(),
                "delta", -litrageValeur,
                "remaining", sb.quantite(type)
        ));

        Bouteille b = new Bouteille();
        b.setType(type);
        b.setLitrage(new Litrage(litrageValeur));
        b.setPrix(prix);
        return b;
    }

    public Boite createBoite(List<Bouteille> list, int quantite, double prix) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("boite sans bouteilles");
        }
        // ensure all bottles same type and litrage
        Type type = list.get(0).getType();
        double litrage = list.get(0).getLitrage().getValue();
        for (Bouteille b : list) {
            if (b.getType() != type) throw new IllegalArgumentException("types de bouteille differs dans la boite");
            if (Double.compare(b.getLitrage().getValue(), litrage) != 0)
                throw new IllegalArgumentException("litrages differs dans la boite");
        }
        int expected = ratioParLitrage(litrage);
        if (list.size() != expected) {
            throw new IllegalArgumentException("ratio incorrect pour carton: attendu=" + expected + ", fourni=" + list.size());
        }
        if (quantite != 0 && quantite != expected) {
            throw new IllegalArgumentException("quantite incoherente avec le ratio: " + expected);
        }
        Boite boite = new Boite();
        boite.setBouteilles(list);
        boite.setQuantite(expected);
        boite.setPrix(prix);
        boite.setType(type);
        return boite;
    }

    private int ratioParLitrage(double litrage) {
        if (Double.compare(litrage, 1.0) == 0) return 15;
        if (Double.compare(litrage, 0.5) == 0) return 30;
        if (Double.compare(litrage, 2.0) == 0) return 8;
        if (Double.compare(litrage, 5.0) == 0) return 6;
        throw new IllegalArgumentException("litrage non supporte pour carton: " + litrage);
    }
}
