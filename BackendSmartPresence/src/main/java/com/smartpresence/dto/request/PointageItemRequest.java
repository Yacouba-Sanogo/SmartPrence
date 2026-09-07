package com.smartpresence.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Pointage individuel de personnel transmis par un lecteur ESP32.
 *
 * <p>L'agent est désigné par son {@code biometricId} — la <b>référence logique</b>
 * associée à son empreinte lors de l'enrôlement. L'ESP32 conserve en NVS la table
 * {@code {slot AS608 → biometricId}} et n'a donc jamais à connaître les identifiants
 * internes du backend (cf. {@code SmartPresence_CONTEXT.md} §8.2). <b>Aucune donnée
 * biométrique n'est transmise</b> : le gabarit d'empreinte ne quitte jamais le capteur.</p>
 *
 * <p>Le <b>sens</b> du pointage (entrée ou sortie) n'est volontairement pas transmis :
 * il est déduit côté serveur à partir des pointages déjà enregistrés pour la journée,
 * ce qui garde le firmware simple et évite les incohérences après une coupure réseau.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointageItemRequest {

    @NotBlank(message = "Le biometricId de l'agent est obligatoire")
    private String biometricId;

    @NotNull(message = "Le deviceId (UUID) est obligatoire")
    private UUID deviceId;

    @NotNull(message = "La date du pointage est obligatoire")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull(message = "L'heure du pointage est obligatoire")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heure;

    @NotNull(message = "Le createdAt du device (horodatage RTC) est obligatoire")
    private Instant createdAt;
}
