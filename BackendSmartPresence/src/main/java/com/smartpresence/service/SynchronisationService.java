package com.smartpresence.service;

import com.smartpresence.dto.response.HistoriqueSynchronisationResponse;

import java.util.List;
import java.util.UUID;

/**
 * Consultation du journal des synchronisations ESP32 → backend.
 *
 * <p>Ce journal était jusqu'ici alimenté sans jamais être exposé : les indicateurs de
 * fiabilité réseau s'accumulaient en base sans qu'aucune interface ne puisse les lire.</p>
 *
 * @since 0.0.1
 */
public interface SynchronisationService {

    /**
     * Journal d'un appareil, du plus récent au plus ancien.
     *
     * @param deviceId appareil concerné
     * @throws com.smartpresence.exception.ResourceNotFoundException si l'appareil est inconnu
     */
    List<HistoriqueSynchronisationResponse> parAppareil(UUID deviceId);

    /**
     * Dernières synchronisations, tous appareils confondus.
     *
     * @param limite nombre maximal d'entrées, borné par le service
     */
    List<HistoriqueSynchronisationResponse> recentes(int limite);
}
