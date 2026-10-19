package com.smartpresence.dto.request;

import com.smartpresence.constants.DiplomeCandidat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

/** Données saisies par un candidat pour savoir s'il peut se présenter au concours. */
public record EligibiliteRequest(
        @NotNull(message = "La date de naissance est obligatoire")
        @Past(message = "La date de naissance doit être passée")
        LocalDate dateNaissance,

        @NotNull(message = "Le diplôme est obligatoire")
        DiplomeCandidat diplome,

        /** Date de référence du calcul d'âge ; aujourd'hui par défaut. */
        LocalDate dateReference) {
}
