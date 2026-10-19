package com.smartpresence.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Requêtes d'administration du référentiel des filières. */
public final class ReferentielRequests {

    private ReferentielRequests() {
    }

    public record Departement(
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 150) String libelle) {
    }

    public record Filiere(
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 150) String libelle,
            @NotNull Long departementId) {
    }

    public record Option(
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 150) String libelle,
            @NotNull Long filiereId) {
    }
}
