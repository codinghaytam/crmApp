package com.jit.agentInterface.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jit.agentInterface.enums.EventType;
import com.jit.agentInterface.model.WebhookSubscription;
import com.jit.agentInterface.repository.WebhookSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebhookDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcherService.class);

    private final WebhookSubscriptionRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public WebhookDispatcherService(WebhookSubscriptionRepository repository) {
        this.repository = repository;
    }

    public void publish(EventType type, Object payload) {
        List<WebhookSubscription> subs = repository.findByActiveTrue();
        if (subs.isEmpty()) return;
        for (WebhookSubscription sub : subs) {
            if (!sub.getEventTypes().isEmpty() && !sub.getEventTypes().contains(type)) continue;
            send(sub, type, payload);
        }
    }

    private void send(WebhookSubscription sub, EventType type, Object payload) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", type.name());
        event.put("timestamp", Instant.now().toString());
        event.put("payload", payload);
        try {
            String json = mapper.writeValueAsString(event);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            restTemplate.postForEntity(sub.getTargetUrl(), entity, String.class);
        } catch (JsonProcessingException e) {
            log.warn("Webhook serialization failed for subscription {}: {}", sub.getId(), e.getMessage());
        } catch (Exception ex) {
            log.warn("Webhook dispatch failed to {}: {}", sub.getTargetUrl(), ex.getMessage());
        }
    }
}

