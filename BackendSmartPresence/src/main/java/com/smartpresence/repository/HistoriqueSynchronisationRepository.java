package com.smartpresence.repository;

import com.smartpresence.constants.StatutSynchronisation;
import com.smartpresence.entity.HistoriqueSynchronisation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Accès aux données de l'historique des synchronisations ESP32 → backend.
 *
 * <p>Sert à la traçabilité et à l'évaluation scientifique du mémoire (fiabilité réseau,
 * nombre de tentatives, performances de synchronisation).</p>
 *
 * @since 0.0.1
 * @see HistoriqueSynchronisation
 */
@Repository
public interface HistoriqueSynchronisationRepository
        extends JpaRepository<HistoriqueSynchronisation, UUID> {

    /**
     * Liste l'historique de synchronisation d'un appareil, du plus récent au plus ancien.
     *
     * @param deviceId identifiant de l'appareil
     * @return liste des entrées d'historique de l'appareil, triées par date décroissante
     */
    List<HistoriqueSynchronisation> findByDeviceIdOrderByDateHeureDesc(UUID deviceId);

    /**
     * Dernières synchronisations, tous appareils confondus.
     *
     * <p>{@code JOIN FETCH} sur l'appareil : chaque ligne affiche son nom, et un
     * chargement paresseux produirait ici un problème N+1.</p>
     *
     * @param limite nombre maximal d'entrées retournées
     */
    @Query("""
            SELECT h FROM HistoriqueSynchronisation h
            JOIN FETCH h.device
            ORDER BY h.dateHeure DESC
            """)
    List<HistoriqueSynchronisation> findRecentes(Pageable limite);

    /** Nombre de synchronisations d'un appareil ayant abouti à un statut donné. */
    long countByDeviceIdAndStatut(UUID deviceId, StatutSynchronisation statut);

}
