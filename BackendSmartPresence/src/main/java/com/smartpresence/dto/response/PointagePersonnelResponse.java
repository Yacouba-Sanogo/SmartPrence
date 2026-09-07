package com.smartpresence.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartpresence.constants.SensPointage;
import com.smartpresence.constants.SourcePresence;
import com.smartpresence.constants.StatutPresence;
import com.smartpresence.constants.TypePersonnel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Pointage unitaire de personnel exposé via l'API REST.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointagePersonnelResponse {

    private UUID id;

    private UUID personnelId;
    private String personnelMatricule;
    private String personnelNom;
    private String personnelPrenom;
    private TypePersonnel personnelType;
    private String personnelService;

    private UUID deviceId;
    private String deviceNom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate datePointage;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heurePointage;

    private SensPointage sens;
    private StatutPresence statut;
    private SourcePresence source;

    /** Justification d'une régularisation ; {@code null} pour un pointage biométrique. */
    private String motif;

    /**
     * Écart en millisecondes entre la capture sur l'appareil et la réception serveur.
     * <p>Métrique de latence de synchronisation exploitée dans l'évaluation du mémoire.</p>
     */
    private Long latenceSynchronisationMs;
}
