package com.smartpresence.dto.response;

import com.smartpresence.constants.StatutSynchronisation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entrée du journal de synchronisation d'un appareil ESP32.
 *
 * <p>Support de la traçabilité et des indicateurs de fiabilité réseau exploités dans
 * l'évaluation du mémoire : volume d'événements transmis, nombre de tentatives avant
 * acquittement, issue de l'opération.</p>
 *
 * <p>Les champs exposés correspondent <b>exactement</b> à ce que l'entité enregistre.
 * Une version antérieure de ce DTO annonçait un début, une fin et une durée d'exécution
 * qu'aucune donnée n'alimentait : MapStruct les laissait à {@code null} sans avertir, et
 * l'API promettait des mesures inexistantes.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueSynchronisationResponse {

    private UUID id;
    private UUID deviceId;
    private String deviceNom;

    /** Instant de réception du lot par le backend. */
    private Instant dateHeure;

    /** Issue de l'opération : succès, partielle ou échec. */
    private StatutSynchronisation statut;

    /** Nombre d'événements contenus dans le lot transmis. */
    private Integer nombreEvenements;

    /**
     * Nombre de tentatives d'envoi côté appareil avant acquittement.
     * <p>Indicateur direct de la qualité du lien réseau du lecteur.</p>
     */
    private Integer nombreTentatives;

    /** Détail des événements rejetés, {@code null} si le lot est passé intégralement. */
    private String messageErreur;

    private Instant createdAt;
}
