package com.jit.agentInterface.service;

import com.jit.agentInterface.enums.EventType;
import com.jit.agentInterface.enums.Role;
import com.jit.agentInterface.enums.VenteType;
import com.jit.agentInterface.model.Vente;
import com.jit.agentInterface.model.User;
import com.jit.agentInterface.repository.BoiteRepository;
import com.jit.agentInterface.repository.BouteilleRepository;
import com.jit.agentInterface.repository.UserRepository;
import com.jit.agentInterface.repository.VenteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class VenteService {

    public record LigneInput(String typeLigne, String produitId, int quantite, double prixUnitaire) {}

    private final VenteRepository venteRepository;
    private final UserRepository userRepository;
    private final BoiteRepository boiteRepository;
    private final BouteilleRepository bouteilleRepository;
    private final WebhookDispatcherService webhookDispatcher;

    public VenteService(VenteRepository venteRepository,
                        UserRepository userRepository,
                        BoiteRepository boiteRepository,
                        BouteilleRepository bouteilleRepository,
                        WebhookDispatcherService webhookDispatcher) {
        this.venteRepository = venteRepository;
        this.userRepository = userRepository;
        this.boiteRepository = boiteRepository;
        this.bouteilleRepository = bouteilleRepository;
        this.webhookDispatcher = webhookDispatcher;
    }

    public Vente createVente(VenteType type, String vendeurId, List<LigneInput> lignes, Authentication auth) {
        // Authorization
        if (type == VenteType.AU_VENDEUR && !hasAnyRole(auth, Role.Agent_commercial, Role.Admin)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "Accès refusé pour type AU_VENDEUR");
        }
        if (type == VenteType.DU_VENDEUR && !hasAnyRole(auth, Role.Vendeur, Role.Admin, Role.Agent_commercial)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "Accès refusé pour type DU_VENDEUR");
        }
        // Vendeur
        Long vid = parseId(vendeurId);
        User vendeur = userRepository.findById(vid).orElseThrow(() -> new ServiceException(HttpStatus.BAD_REQUEST, "vendeur introuvable"));
        if (vendeur.getRole() != Role.Vendeur) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "l'utilisateur cible n'est pas un vendeur");
        }
        // Construire la vente
        Vente vente = new Vente();
        vente.setType(type);
        vente.setVendeur(vendeur);
        List<Vente.Ligne> lignesVente = new ArrayList<>();
        for (LigneInput li : lignes) {
            String tl = li.typeLigne() == null ? null : li.typeLigne().toUpperCase(Locale.ROOT);
            switch (type) {
                case AU_VENDEUR -> {
                    if (!Objects.equals(tl, "BOITE") && !Objects.equals(tl, "BOUTEILLE"))
                        throw new ServiceException(HttpStatus.BAD_REQUEST, "typeLigne invalide pour AU_VENDEUR: " + li.typeLigne());
                    Long pid = parseId(li.produitId());
                    if (Objects.equals(tl, "BOITE")) {
                        boiteRepository.findById(pid).orElseThrow(() -> new ServiceException(HttpStatus.BAD_REQUEST, "boite inconnue: " + pid));
                    } else {
                        bouteilleRepository.findById(pid).orElseThrow(() -> new ServiceException(HttpStatus.BAD_REQUEST, "bouteille inconnue: " + pid));
                    }
                    lignesVente.add(new Vente.Ligne(tl, li.produitId(), li.quantite(), li.prixUnitaire()));
                }
                case DU_VENDEUR -> {
                    if (!Objects.equals(tl, "BOUTEILLE"))
                        throw new ServiceException(HttpStatus.BAD_REQUEST, "typeLigne doit être BOUTEILLE pour DU_VENDEUR");
                    Long bid = parseId(li.produitId());
                    bouteilleRepository.findById(bid).orElseThrow(() -> new ServiceException(HttpStatus.BAD_REQUEST, "bouteille inconnue: " + bid));
                    lignesVente.add(new Vente.Ligne("BOUTEILLE", li.produitId(), li.quantite(), li.prixUnitaire()));
                }
            }
        }
        vente.setLignes(lignesVente);
        Vente saved = venteRepository.save(vente);
        // publish webhook
        EventType eventType = (saved.getType() == VenteType.AU_VENDEUR) ? EventType.VENTE_AU_VENDEUR_CREATED : EventType.VENTE_DU_VENDEUR_CREATED;
        webhookDispatcher.publish(eventType, Map.of(
                "venteId", saved.getId(),
                "vendeurId", saved.getVendeurId(),
                "type", saved.getType().name(),
                "montantTotal", saved.getMontantTotal()
        ));
        return saved;
    }

    public List<Vente> listVentes(VenteType type, LocalDate dateDebut, LocalDate dateFin, String vendeurId) {
        Instant start = dateDebut != null ? dateDebut.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant end = dateFin != null ? dateFin.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        if (type != null && vendeurId != null && start != null && end != null) {
            return venteRepository.findByTypeAndVendeur_IdAndDateVenteBetween(type, parseId(vendeurId), start, end);
        } else if (type != null && vendeurId != null) {
            return venteRepository.findByTypeAndVendeur_Id(type, parseId(vendeurId));
        } else if (type != null && start != null && end != null) {
            return venteRepository.findByTypeAndDateVenteBetween(type, start, end);
        } else if (type != null) {
            return venteRepository.findByType(type);
        } else if (vendeurId != null && start != null && end != null) {
            return venteRepository.findByVendeur_IdAndDateVenteBetween(parseId(vendeurId), start, end);
        } else if (vendeurId != null) {
            return venteRepository.findByVendeur_Id(parseId(vendeurId));
        } else if (start != null && end != null) {
            return venteRepository.findByDateVenteBetween(start, end);
        }
        return venteRepository.findAll();
    }

    public Optional<Vente> getVente(String id) {
        return venteRepository.findById(id);
    }

    private Long parseId(String raw) {
        try { return Long.parseLong(raw); } catch (NumberFormatException e) { throw new ServiceException(HttpStatus.BAD_REQUEST, "identifiant numérique invalide: " + raw); }
    }

    private boolean hasAnyRole(Authentication auth, Role... roles) {
        if (auth == null) return false;
        Set<String> wanted = new HashSet<>();
        for (Role r : roles) wanted.add("ROLE_" + r.name());
        return auth.getAuthorities().stream().anyMatch(a -> wanted.contains(a.getAuthority()));
    }
}
