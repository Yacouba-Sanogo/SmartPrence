package com.smartpresence.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartpresence.constants.StatutPresence;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Requête de création/saisie manuelle d'une présence (par un enseignant ou administrateur).
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceManuelRequest {

    @NotNull(message = "L'identifiant de l'étudiant est obligatoire")
    private UUID etudiantId;

    private UUID deviceId; // Optionnel si source = MANUEL

    /** Séance concernée, obligatoire pour une correction de présence académique contextualisée. */
    private UUID seanceId;

    @NotNull(message = "La date est obligatoire")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate datePresence;

    @NotNull(message = "L'heure est obligatoire")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heurePresence;

    @NotNull(message = "Le statut est obligatoire")
    private StatutPresence statut;
}
