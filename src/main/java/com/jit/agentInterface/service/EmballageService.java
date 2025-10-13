package com.jit.agentInterface.service;

import com.jit.agentInterface.enums.EventType;
import com.jit.agentInterface.model.Boite;
import com.jit.agentInterface.model.Chariot;
import com.jit.agentInterface.repository.BoiteRepository;
import com.jit.agentInterface.repository.ChariotRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmballageService {

    private final ChariotRepository chariotRepository;
    private final BoiteRepository boiteRepository;
    private final WebhookDispatcherService webhookDispatcherService;

    public EmballageService(ChariotRepository chariotRepository, BoiteRepository boiteRepository, WebhookDispatcherService webhookDispatcherService) {
        this.chariotRepository = chariotRepository;
        this.boiteRepository = boiteRepository;
        this.webhookDispatcherService = webhookDispatcherService;
    }

    public long addChariot() {
        chariotRepository.save(new Chariot());
        long count = chariotRepository.count();
        publishPackagingEvent("ADD_CHARIOT");
        return count;
    }

    public long removeChariotByIndex(int index) {
        List<Chariot> list = allChariotsOrdered();
        if (index < 0 || index >= list.size()) throw new ServiceException(HttpStatus.BAD_REQUEST, "chariot introuvable");
        chariotRepository.delete(list.get(index));
        long count = chariotRepository.count();
        publishPackagingEvent("REMOVE_CHARIOT");
        return count;
    }

    public int addBoiteToChariot(int index, Boite boite) {
        List<Chariot> list = allChariotsOrdered();
        if (index < 0 || index >= list.size()) throw new ServiceException(HttpStatus.BAD_REQUEST, "chariot introuvable");
        Chariot c = list.get(index);
        Boite savedBoite = boite.getId() == null ? boiteRepository.save(boite) : boite;
        if (c.getBoites() == null) c.setBoites(new ArrayList<>());
        c.getBoites().add(savedBoite);
        chariotRepository.save(c);
        int total = totalBoites();
        publishPackagingEvent("ADD_BOITE");
        return total;
    }

    public int removeBoiteFromChariot(int index, int indexBoite) {
        List<Chariot> list = allChariotsOrdered();
        if (index < 0 || index >= list.size()) throw new ServiceException(HttpStatus.BAD_REQUEST, "chariot introuvable");
        Chariot c = list.get(index);
        List<Boite> boites = c.getBoites();
        if (boites == null || indexBoite < 0 || indexBoite >= boites.size()) throw new ServiceException(HttpStatus.BAD_REQUEST, "boite introuvable");
        boites.remove(indexBoite);
        chariotRepository.save(c);
        int total = totalBoites();
        publishPackagingEvent("REMOVE_BOITE");
        return total;
    }

    public List<Chariot> allChariotsOrdered() {
        return chariotRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public int totalBoites() {
        return allChariotsOrdered().stream().mapToInt(ch -> ch.getBoites() == null ? 0 : ch.getBoites().size()).sum();
    }

    public Map<String, Object> summary() {
        Map<String, Object> m = new HashMap<>();
        m.put("chariots", chariotRepository.count());
        m.put("boitesEmballees", totalBoites());
        return m;
    }

    private void publishPackagingEvent(String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("category", "EMBALLAGE");
        payload.put("action", action);
        payload.put("chariots", chariotRepository.count());
        payload.put("boitesEmballees", totalBoites());
        webhookDispatcherService.publish(EventType.STOCK_CHANGED, payload);
    }
}
