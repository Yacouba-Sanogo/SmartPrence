package com.smartpresence.dto.request;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.constants.TypeEvaluation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Saisie d'une note par un enseignant.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRequest {

    @NotNull(message = "L'étudiant est obligatoire")
    private UUID etudiantId;

    @NotNull(message = "La matière est obligatoire")
    private Long matiereId;

    /**
     * Valeur sur 20.
     *
     * <p>Les bornes sont vérifiées ici <b>et</b> par la base : une note hors barème
     * fausserait toutes les moyennes qui en dépendent, et rien ne la signalerait.</p>
     */
    @NotNull(message = "La note est obligatoire")
    @DecimalMin(value = "0.0", message = "La note ne peut pas être négative")
    @DecimalMax(value = "20.0", message = "La note ne peut pas dépasser 20")
    private BigDecimal valeur;

    @Min(value = 1, message = "Le coefficient vaut au moins 1")
    @Max(value = 20, message = "Le coefficient ne peut pas dépasser 20")
    private Integer coefficient;

    private TypeEvaluation type;

    private PeriodeScolaire periode;

    @NotBlank(message = "L'intitulé de l'évaluation est obligatoire")
    @Size(max = 120, message = "L'intitulé ne peut pas dépasser 120 caractères")
    private String libelle;

    @NotNull(message = "La date de l'évaluation est obligatoire")
    private LocalDate dateEvaluation;

    @Size(max = 500, message = "L'appréciation ne peut pas dépasser 500 caractères")
    private String appreciation;
}
