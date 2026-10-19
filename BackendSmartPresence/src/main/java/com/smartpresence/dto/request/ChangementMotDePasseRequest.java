package com.smartpresence.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Changement de mot de passe par son titulaire. */
public record ChangementMotDePasseRequest(
        @NotBlank(message = "Saisissez votre mot de passe actuel") String motDePasseActuel,
        @NotBlank(message = "Saisissez le nouveau mot de passe")
        @Size(min = 8, max = 72, message = "Le nouveau mot de passe doit contenir entre 8 et 72 caractères")
        String nouveauMotDePasse) { }
