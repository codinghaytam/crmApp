package com.jit.agentInterface.service;

import com.jit.agentInterface.enums.EventType;
import com.jit.agentInterface.model.WebhookSubscription;
import com.jit.agentInterface.repository.WebhookSubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class WebhookSubscriptionService {

    private final WebhookSubscriptionRepository repository;

    public WebhookSubscriptionService(WebhookSubscriptionRepository repository) {
        this.repository = repository;
    }

    public WebhookSubscription create(String targetUrl, Set<EventType> eventTypes) {
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "targetUrl requis");
        }
        WebhookSubscription sub = new WebhookSubscription();
        sub.setTargetUrl(targetUrl);
        if (eventTypes != null) sub.setEventTypes(eventTypes);
        return repository.save(sub);
    }

    public List<WebhookSubscription> list() {
        return repository.findAll();
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ServiceException(HttpStatus.NOT_FOUND, "subscription introuvable");
        }
        repository.deleteById(id);
    }
}

