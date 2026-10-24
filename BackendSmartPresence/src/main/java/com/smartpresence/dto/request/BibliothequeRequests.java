package com.smartpresence.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Requêtes de la bibliothèque. */
public final class BibliothequeRequests {

    private BibliothequeRequests() { }

    public record Livre(
            @NotBlank @Size(max = 200) String titre,
            @NotBlank @Size(max = 150) String auteur,
            @Size(max = 30) String isbn,
            @Size(max = 80) String categorie,
            Long filiereId,
            @Size(max = 500) String resume,
            @Size(max = 80) String emplacement,
            @NotNull @Min(1) @Max(500) Integer exemplaires) { }

    /** {@code dureeJours} absent : la durée par défaut de la bibliothèque s'applique. */
    public record Emprunt(
            @NotNull UUID livreId,
            @NotNull UUID etudiantId,
            @Min(1) @Max(60) Integer dureeJours) { }

    public record Prolongation(@NotNull @Min(1) @Max(30) Integer jours) { }

    /** Décision sur un document déposé ; le motif est exigé en cas de refus. */
    public record Decision(@NotNull Boolean accepte, @Size(max = 500) String motif) { }
}
