package com.smartpresence.service;

import com.smartpresence.constants.StatutSignalement;
import com.smartpresence.dto.request.SignalementRequest;
import com.smartpresence.dto.request.TraitementSignalementRequest;
import com.smartpresence.dto.response.SignalementResponse;

import java.util.List;
import java.util.UUID;

/**
 * Signalements d'anomalie sur les relevés de présence.
 *
 * <p>Le circuit est volontairement en deux temps : l'enseignant témoigne, la scolarité
 * arbitre. Aucune correction n'est appliquée par celui qui la demande.</p>
 *
 * @since 0.0.1
 */
public interface SignalementService {

    /**
     * Dépose un signalement sur une séance.
     *
     * @param utilisateurId enseignant connecté, déduit du jeton
     * @param seanceId      séance concernée — doit être assurée par cet enseignant
     */
    SignalementResponse signaler(UUID utilisateurId, UUID seanceId, SignalementRequest request);

    /** Signalements déposés par l'enseignant connecté. */
    List<SignalementResponse> mesSignalements(UUID utilisateurId);

    /**
     * File d'arbitrage de la scolarité.
     *
     * @param statut filtre facultatif ; {@code null} pour tout voir
     */
    List<SignalementResponse> rechercher(StatutSignalement statut);

    /**
     * Arbitre un signalement.
     *
     * <p>Une acceptation de type {@code ETUDIANT_NON_RECONNU} produit un relevé de
     * présence de source {@code MANUEL}, rattaché au signalement — la correction reste
     * ainsi distinguable d'une identification biométrique et remonte à sa justification.</p>
     *
     * @param arbitreId agent de la scolarité qui tranche
     */
    SignalementResponse traiter(UUID signalementId, UUID arbitreId,
                                TraitementSignalementRequest request);
}
