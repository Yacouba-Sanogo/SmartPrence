package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Statistiques individuelles d'assiduité pour un étudiant.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesEtudiantResponse {

    private UUID etudiantId;
    private String matricule;
    private String nom;
    private String prenom;
    private String classeCode;
    private long totalPresences;
    private long totalPresents;
    private long totalRetards;
    private long totalAbsents;
    private long totalJustifies;
    private double tauxAssiduite;
}
