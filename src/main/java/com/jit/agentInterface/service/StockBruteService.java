package com.jit.agentInterface.service;

import com.jit.agentInterface.enums.EventType;
import com.jit.agentInterface.enums.Type;
import com.jit.agentInterface.model.StockBrute;
import com.jit.agentInterface.repository.StockBruteRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class StockBruteService {

    private final StockBruteRepository repository;
    private final WebhookDispatcherService webhookDispatcher;

    public StockBruteService(StockBruteRepository repository, WebhookDispatcherService webhookDispatcher) {
        this.repository = repository;
        this.webhookDispatcher = webhookDispatcher;
    }

    private StockBrute getOrCreate() {
        return repository.findAll().stream().findFirst().orElseGet(() -> repository.save(new StockBrute()));
    }

    public BigDecimal augmenter(Type type, BigDecimal quantite) {
        var sb = getOrCreate();
        sb.augmenterQuantite(type, quantite);
        repository.save(sb);
        webhookDispatcher.publish(EventType.STOCK_CHANGED, Map.of(
                "type", type.name(),
                "delta", quantite,
                "remaining", sb.quantite(type)
        ));
        return sb.quantite(type);
    }

    public BigDecimal diminuer(Type type, BigDecimal quantite) {
        var sb = getOrCreate();
        sb.diminuerQuantite(type, quantite);
        repository.save(sb);
        webhookDispatcher.publish(EventType.STOCK_CHANGED, Map.of(
                "type", type.name(),
                "delta", quantite.negate(),
                "remaining", sb.quantite(type)
        ));
        return sb.quantite(type);
    }

    public BigDecimal quantite(Type type) {
        var sb = getOrCreate();
        return sb.quantite(type);
    }
}

