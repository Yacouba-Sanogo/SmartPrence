package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Indicateurs globaux pour le tableau de bord (Flutter).
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesGlobalesResponse {

    private long totalEtudiants;
    private long totalClasses;
    private long totalDevicesActifs;
    private long totalPresencesEnregistrees;
    private long totalPresents;
    private long totalRetards;
    private long totalAbsents;
    private long totalJustifies;
    private double tauxAssiduite; // Pourcentage
}
