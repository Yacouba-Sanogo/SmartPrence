package com.smartpresence.dto.request;

import com.smartpresence.constants.PeriodeScolaire;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Création ou modification d'une unité d'enseignement.
 *
 * @since 0.0.1
 */
@Getter
@Setter
public class UniteEnseignementRequest {

    @NotBlank(message = "Le code de l'UE est obligatoire")
    @Size(max = 50, message = "Le code ne peut pas dépasser 50 caractères")
    private String code;

    @NotBlank(message = "Le libellé de l'UE est obligatoire")
    @Size(max = 150, message = "Le libellé ne peut pas dépasser 150 caractères")
    private String libelle;

    /** Crédits rapportés par l'UE — 30 par semestre, répartis entre les UE. */
    @NotNull(message = "Le nombre de crédits est obligatoire")
    @Min(value = 1, message = "Une UE rapporte au moins 1 crédit")
    @Max(value = 60, message = "Le nombre de crédits est invraisemblable")
    private Integer credits;

    @NotNull(message = "Le semestre est obligatoire")
    private PeriodeScolaire semestre;

    @NotNull(message = "La promotion est obligatoire")
    private Long promotionId;

    private Boolean active;
}
