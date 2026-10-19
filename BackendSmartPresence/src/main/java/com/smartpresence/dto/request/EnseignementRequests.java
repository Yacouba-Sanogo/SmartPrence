package com.smartpresence.dto.request;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.constants.TypeEvaluation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Requêtes de l'espace enseignant au-delà de la présence : cahier de textes, syllabus,
 * évaluations et circuit de validation des notes.
 */
public final class EnseignementRequests {

    private EnseignementRequests() { }

    /** Page du cahier de textes d'une séance. */
    public record Cahier(
            @NotBlank(message = "Le contenu enseigné est obligatoire")
            @Size(max = 2000, message = "Le contenu ne peut pas dépasser 2000 caractères") String contenu,
            @Size(max = 500, message = "Le travail à faire ne peut pas dépasser 500 caractères") String travailAFaire,
            UUID chapitreId,
            boolean chapitreTermine) { }

    public record NouveauChapitre(
            @NotNull(message = "La classe est obligatoire") Long classeId,
            @NotNull(message = "La matière est obligatoire") Long matiereId,
            @NotBlank(message = "Le titre est obligatoire")
            @Size(max = 150, message = "Le titre ne peut pas dépasser 150 caractères") String titre,
            @Size(max = 500, message = "Les objectifs ne peuvent pas dépasser 500 caractères") String objectifs) { }

    public record ModificationChapitre(
            @NotBlank(message = "Le titre est obligatoire")
            @Size(max = 150, message = "Le titre ne peut pas dépasser 150 caractères") String titre,
            @Size(max = 500, message = "Les objectifs ne peuvent pas dépasser 500 caractères") String objectifs,
            boolean termine) { }

    /**
     * Évaluation à planifier. {@code classeId} n'est lu qu'à la création : déplacer une
     * évaluation vers une autre classe laisserait ses notes chez de mauvais étudiants.
     */
    public record Evaluation(
            Long classeId,
            @NotNull(message = "La matière est obligatoire") Long matiereId,
            TypeEvaluation type,
            @NotBlank(message = "L'intitulé est obligatoire")
            @Size(max = 120, message = "L'intitulé ne peut pas dépasser 120 caractères") String libelle,
            @NotNull(message = "La date est obligatoire") LocalDate dateEvaluation,
            @Min(value = 1, message = "Le barème vaut au moins 1")
            @Max(value = 100, message = "Le barème ne peut pas dépasser 100") Integer bareme,
            @Min(value = 1, message = "Le coefficient vaut au moins 1")
            @Max(value = 20, message = "Le coefficient ne peut pas dépasser 20") Integer coefficient,
            PeriodeScolaire periode,
            @Size(max = 2000, message = "Le sujet ne peut pas dépasser 2000 caractères") String sujet) { }

    /** Résultat d'un étudiant : une valeur sur le barème de l'évaluation, ou une absence. */
    public record Resultat(
            @NotNull(message = "L'étudiant est obligatoire") UUID etudiantId,
            @DecimalMin(value = "0.0", message = "La note ne peut pas être négative")
            @DecimalMax(value = "100.0", message = "La note dépasse tout barème possible") BigDecimal valeur,
            boolean absent,
            @Size(max = 300, message = "Le motif ne peut pas dépasser 300 caractères") String motif,
            @Size(max = 500, message = "L'appréciation ne peut pas dépasser 500 caractères") String appreciation) { }

    public record Resultats(
            @NotEmpty(message = "Aucun résultat à enregistrer") List<@Valid Resultat> resultats) { }

    /** Lot de notes transmis, validé ou renvoyé en une seule opération. */
    public record LotNotes(
            @NotEmpty(message = "Aucune note sélectionnée") List<UUID> noteIds,
            @Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères") String motif) { }
}
