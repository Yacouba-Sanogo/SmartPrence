package com.smartpresence.service;

import com.smartpresence.dto.request.PointageESP32Request;
import com.smartpresence.dto.request.PointageManuelRequest;
import com.smartpresence.dto.response.JourneePersonnelResponse;
import com.smartpresence.dto.response.PointagePersonnelResponse;
import com.smartpresence.dto.response.SyncSummaryResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Contrat métier du pointage horaire du personnel.
 *
 * <p>Couvre l'ingestion des identifications biométriques transmises par les lecteurs
 * d'entrée, leur régularisation administrative, et la restitution exploitable de la
 * journée de travail (heure d'arrivée, retard, temps de présence).</p>
 *
 * @since 0.0.1
 */
public interface PointagePersonnelService {

    /**
     * Ingère un lot de pointages transmis par un lecteur ESP32.
     *
     * <p>Opération <b>idempotente</b> : un lot retransmis après une coupure réseau
     * n'entraîne aucun doublon. Un élément en erreur (agent inconnu, agent inactif,
     * appareil incohérent) n'interrompt pas le traitement des autres.</p>
     *
     * @param authenticatedDeviceId appareil authentifié par sa clé d'API
     * @param request               lot de pointages
     * @return résumé de la synchronisation destiné à l'appareil
     */
    SyncSummaryResponse synchronize(UUID authenticatedDeviceId, PointageESP32Request request);

    /**
     * Enregistre une régularisation administrative d'un pointage.
     *
     * @param request pointage à créer, de source {@code MANUEL}
     * @return le pointage enregistré
     */
    PointagePersonnelResponse createManuel(PointageManuelRequest request);

    /**
     * Synthèse de la journée pour <b>tous</b> les agents actifs.
     *
     * <p>Les agents n'ayant produit aucun pointage sont restitués avec le statut
     * {@code ABSENT} : c'est précisément l'information attendue par un service RH.</p>
     *
     * @param date journée observée
     * @return une ligne de synthèse par agent actif
     */
    List<JourneePersonnelResponse> journee(LocalDate date);

    /**
     * Flux brut des pointages d'une journée, du plus récent au plus ancien.
     *
     * @param date journée observée
     * @return pointages unitaires de la journée
     */
    List<PointagePersonnelResponse> pointagesDuJour(LocalDate date);

    /**
     * Historique des journées d'un agent sur une période.
     *
     * @param personnelId agent concerné
     * @param debut       premier jour inclus
     * @param fin         dernier jour inclus
     * @return une ligne de synthèse par jour travaillé
     */
    List<JourneePersonnelResponse> historique(UUID personnelId, LocalDate debut, LocalDate fin);
}
