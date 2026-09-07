package com.smartpresence.dto.response;

import com.smartpresence.constants.SourcePresence;
import com.smartpresence.constants.StatutPresence;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Données d'un événement de présence exposées via l'API REST.
 *
 * <p>Calcule la <b>latence de synchronisation (en millisecondes)</b> entre la capture RTC
 * ESP32 ({@code creeLeDevice}) et la persistance serveur ({@code synchroniseLe}).
 * Cette métrique est essentielle pour l'évaluation scientifique du mémoire.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceResponse {

    private UUID id;

    // Etudiant
    private UUID etudiantId;
    private String etudiantMatricule;
    private String etudiantNom;
    private String etudiantPrenom;
    private String classeCode;

    // Device
    private UUID deviceId;
    private String deviceNom;
    private String salleNom;
    private UUID seanceId;

    /**
     * Matière de la séance rattachée, {@code null} pour un pointage hors créneau identifié.
     *
     * <p>Sans elle, un relevé se réduit à « telle salle, telle heure » : c'est le cours
     * qui donne son sens à la ligne pour l'étudiant qui consulte son historique.</p>
     */
    private String matiereLibelle;

    // Date & Heure
    private LocalDate datePresence;
    private LocalTime heurePresence;

    // Statut & Source
    private StatutPresence statut;
    private SourcePresence source;

    // Métriques d'horodatage
    private Instant creeLeDevice;
    private Instant synchroniseLe;
    private Long latenceSynchronisationMs;

    private Instant createdAt;
    private Instant updatedAt;
}
