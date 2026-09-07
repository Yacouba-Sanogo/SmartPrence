package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Statistiques d'assiduité agrégées pour une classe.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesClasseResponse {

    private Long classeId;
    private String classeCode;
    private String classeLibelle;
    private long totalEtudiants;
    private long totalPresences;
    private long totalPresents;
    private long totalRetards;
    private long totalAbsents;
    private long totalJustifies;
    private double tauxAssiduite;
}
