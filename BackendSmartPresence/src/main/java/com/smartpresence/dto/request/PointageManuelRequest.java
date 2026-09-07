package com.smartpresence.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartpresence.constants.SensPointage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Régularisation administrative d'un pointage de personnel.
 *
 * <p>Couvre les cas réels où le lecteur n'a pas pu opérer : panne du capteur, doigt
 * non reconnu de façon répétée, agent en mission externe. La source enregistrée est
 * alors {@code MANUEL} et l'appareil est facultatif, ce qui rend la correction
 * <b>traçable et distinguable</b> d'un pointage biométrique authentique.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointageManuelRequest {

    @NotNull(message = "L'identifiant de l'agent est obligatoire")
    private UUID personnelId;

    @NotNull(message = "La date du pointage est obligatoire")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate datePointage;

    @NotNull(message = "L'heure du pointage est obligatoire")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heurePointage;

    /** Entrée ou sortie. Contrairement au flux ESP32, le sens est ici explicite. */
    @NotNull(message = "Le sens du pointage (ENTREE ou SORTIE) est obligatoire")
    private SensPointage sens;

    /** Appareil concerné si la régularisation se rattache à un lecteur identifié. */
    private UUID deviceId;

    /**
     * Justification de la saisie manuelle — <b>obligatoire</b>.
     *
     * <p>Exiger un motif est ce qui distingue une correction traçable d'une
     * altération silencieuse du relevé de présence.</p>
     */
    @NotBlank(message = "Le motif de la régularisation est obligatoire")
    @Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères")
    private String motif;
}
