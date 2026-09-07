package com.smartpresence.dto.response;

import com.smartpresence.constants.StatutJustification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Données d'une justification d'absence exposées via l'API.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JustificationAbsenceResponse {

    private UUID id;
    private UUID etudiantId;
    private String etudiantNom;
    private String etudiantPrenom;
    private UUID seanceId;
    private LocalDate dateAbsence;
    private String motif;
    private String pieceJointeUrl;
    private StatutJustification statut;
    private UUID traiteParId;
    private String commentaireTraitement;
    private Instant createdAt;
    private Instant updatedAt;
}
