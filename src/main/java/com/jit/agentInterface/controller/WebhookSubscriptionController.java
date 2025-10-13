package com.jit.agentInterface.controller;

import com.jit.agentInterface.enums.EventType;
import com.jit.agentInterface.model.WebhookSubscription;
import com.jit.agentInterface.service.WebhookSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/webhooks/subscriptions")
@Tag(name = "Webhooks")
public class WebhookSubscriptionController {

    private final WebhookSubscriptionService service;

    public WebhookSubscriptionController(WebhookSubscriptionService service) {
        this.service = service;
    }

    public record CreateSubscriptionRequest(@NotBlank String targetUrl, Set<EventType> eventTypes) {}
    public record SubscriptionResponse(Long id, String targetUrl, boolean active, Set<EventType> eventTypes) {}

    @PostMapping
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Créer une souscription webhook", description = "Si eventTypes est vide ou null => tous les événements seront envoyés.")
    public ResponseEntity<SubscriptionResponse> create(@Valid @RequestBody CreateSubscriptionRequest req) {
        WebhookSubscription saved = service.create(req.targetUrl(), req.eventTypes());
        return ResponseEntity.created(URI.create("/api/webhooks/subscriptions/" + saved.getId()))
                .body(toResponse(saved));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Lister les souscriptions webhook actives")
    public ResponseEntity<List<SubscriptionResponse>> list() {
        return ResponseEntity.ok(service.list().stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole(T(com.jit.agentInterface.enums.Role).Admin.name())")
    @Operation(summary = "Supprimer une souscription webhook")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private SubscriptionResponse toResponse(WebhookSubscription sub) {
        return new SubscriptionResponse(sub.getId(), sub.getTargetUrl(), sub.isActive(), sub.getEventTypes());
    }
}
