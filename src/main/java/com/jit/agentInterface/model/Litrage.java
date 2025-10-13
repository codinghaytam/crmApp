package com.jit.agentInterface.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Litrage {
    private double value;

    public Litrage() {}

    public Litrage(double value) {
        if (value != 1.0 && value != 0.5 && value != 2.0 && value != 5.0) {
            throw new IllegalArgumentException("litrage non supporte: " + value);
        }
        this.value = value;
    }

    public double getValue() { return value; }
}
