package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Données d'un étudiant exposées via l'API.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtudiantResponse {

    private UUID id;
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private LocalDate dateNaissance;
    private String biometricId;

    /** {@code true} si l'étudiant dispose d'une empreinte enrôlée. */
    private boolean enrole;
    private boolean actif;
    private Long classeId;
    private String classeCode;
    private String classeLibelle;
    private String promotionLibelle;

    /** Compte de connexion associé, {@code null} si l'étudiant n'a pas d'accès mobile. */
    private UUID utilisateurId;

    /** {@code true} si l'étudiant peut se connecter à l'application mobile. */
    private boolean compteOuvert;
    private Instant createdAt;
    private Instant updatedAt;
}
