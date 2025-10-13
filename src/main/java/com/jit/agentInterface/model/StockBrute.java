package com.jit.agentInterface.model;

import com.jit.agentInterface.enums.Type;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@Entity
public class StockBrute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "stock_brut_quantites", joinColumns = @JoinColumn(name = "stock_brut_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "quantite")
    private Map<Type, BigDecimal> stock;

    public StockBrute() {
        this.stock = new EnumMap<>(Type.class);
        for (Type t : Type.values()) { this.stock.put(t, BigDecimal.ZERO); }
    }

    @PrePersist
    public void prePersist() {
        if (stock == null) {
            stock = new EnumMap<>(Type.class);
            for (Type t : Type.values()) { stock.put(t, BigDecimal.ZERO); }
        }
    }

    public Long getId() { return id; }

    public BigDecimal quantite(Type type) { return stock.getOrDefault(type, BigDecimal.ZERO); }

    public void augmenterQuantite(Type type, BigDecimal qte) {
        Objects.requireNonNull(type, "type null");
        Objects.requireNonNull(qte, "qte null");
        if (qte.signum() < 0) throw new IllegalArgumentException("qte negative");
        stock.put(type, quantite(type).add(qte));
    }

    public void diminuerQuantite(Type type, BigDecimal qte) {
        Objects.requireNonNull(type, "type null");
        Objects.requireNonNull(qte, "qte null");
        if (qte.signum() < 0) throw new IllegalArgumentException("qte negative");
        var actuelle = quantite(type);
        if (actuelle.compareTo(qte) < 0) throw new IllegalArgumentException("stock insuffisant");
        stock.put(type, actuelle.subtract(qte));
    }
}
