package com.smartpresence.dto.request;

import jakarta.validation.constraints.NotNull;

/** Requêtes de parcours : changement de classe et choix d'option. */
public final class ParcoursRequests {

    private ParcoursRequests() {
    }

    public record ChangementClasse(@NotNull(message = "La classe est obligatoire") Long classeId) {
    }

    public record ChoixOption(@NotNull(message = "L'option est obligatoire") Long optionId) {
    }
}
