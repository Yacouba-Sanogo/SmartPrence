package com.smartpresence.dto.response;

import com.smartpresence.constants.StatutSignalement;
import com.smartpresence.constants.TypeSignalement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Signalement d'anomalie exposé via l'API.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalementResponse {

    private UUID id;

    private UUID seanceId;
    private String matiereLibelle;
    private String classeCode;
    private Instant seanceDebut;

    private UUID enseignantId;
    private String enseignantNom;

    /** Étudiant visé, {@code null} pour une anomalie touchant la séance entière. */
    private UUID etudiantId;
    private String etudiantNom;
    private String etudiantMatricule;

    private TypeSignalement type;
    private String description;
    private StatutSignalement statut;

    private String commentaireTraitement;
    private Instant traiteLe;

    /** Relevé correctif produit à l'acceptation, {@code null} sinon. */
    private UUID presenceCorrectiveId;

    private Instant createdAt;
}
