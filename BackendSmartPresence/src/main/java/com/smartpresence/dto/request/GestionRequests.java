package com.smartpresence.dto.request;

import com.smartpresence.constants.TypeConge;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Demandes de congé et contestations de notes. */
public final class GestionRequests {

    private GestionRequests() { }

    public record Conge(
            TypeConge type,
            @NotNull(message = "La date de début est obligatoire") LocalDate debut,
            @NotNull(message = "La date de fin est obligatoire") LocalDate fin,
            @NotBlank(message = "Le motif est obligatoire")
            @Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères") String motif) { }

    /** Décision de l'administration ; le refus doit être motivé. */
    public record DecisionConge(
            boolean accepter,
            @Size(max = 500, message = "La réponse ne peut pas dépasser 500 caractères") String reponse) { }

    public record Contestation(
            @NotBlank(message = "Expliquez ce que vous contestez")
            @Size(max = 1000, message = "Le motif ne peut pas dépasser 1000 caractères") String motif) { }

    /**
     * Réponse de l'enseignant. {@code valeurCorrigee} n'a de sens que si la réclamation
     * est acceptée : elle remplace alors la note, et la réclamation garde l'ancienne.
     */
    public record ReponseReclamation(
            boolean accepter,
            @NotBlank(message = "Répondez à l'étudiant")
            @Size(max = 1000, message = "La réponse ne peut pas dépasser 1000 caractères") String reponse,
            @DecimalMin(value = "0.0", message = "La note ne peut pas être négative")
            @DecimalMax(value = "20.0", message = "La note ne peut pas dépasser 20") BigDecimal valeurCorrigee) { }
}
