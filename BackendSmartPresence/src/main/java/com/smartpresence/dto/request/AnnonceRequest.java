package com.smartpresence.dto.request;

import com.smartpresence.constants.TypeNotification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Annonce de l'administration. Sans {@code classeId} ni {@code personnelId}, elle part à
 * tous les enseignants actifs ; avec {@code personnelId}, à lui seul.
 */
public record AnnonceRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 160, message = "Le titre ne peut pas dépasser 160 caractères") String titre,
        @NotBlank(message = "Le message est obligatoire")
        @Size(max = 2000, message = "Le message ne peut pas dépasser 2000 caractères") String message,
        TypeNotification type,
        Long classeId,
        UUID personnelId) { }
