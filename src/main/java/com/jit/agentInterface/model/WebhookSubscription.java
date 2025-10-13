package com.jit.agentInterface.model;

import com.jit.agentInterface.enums.EventType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
public class WebhookSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String targetUrl;

    private boolean active = true;

    private Instant createdAt = Instant.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "webhook_subscription_events", joinColumns = @JoinColumn(name = "subscription_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 50)
    private Set<EventType> eventTypes = new HashSet<>();

    public Long getId() { return id; }
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Set<EventType> getEventTypes() { return eventTypes; }
    public void setEventTypes(Set<EventType> eventTypes) { this.eventTypes = eventTypes; }
}

