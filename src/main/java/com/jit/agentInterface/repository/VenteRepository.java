package com.jit.agentInterface.repository;

import com.jit.agentInterface.enums.VenteType;
import com.jit.agentInterface.model.Vente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface VenteRepository extends JpaRepository<Vente, String> {
    List<Vente> findByType(VenteType type);
    List<Vente> findByVendeur_Id(Long vendeurId);
    List<Vente> findByDateVenteBetween(Instant debut, Instant fin);
    List<Vente> findByVendeur_IdAndDateVenteBetween(Long vendeurId, Instant debut, Instant fin);
    List<Vente> findByTypeAndDateVenteBetween(VenteType type, Instant debut, Instant fin);
    List<Vente> findByTypeAndVendeur_Id(VenteType type, Long vendeurId);
    List<Vente> findByTypeAndVendeur_IdAndDateVenteBetween(VenteType type, Long vendeurId, Instant debut, Instant fin);
}
