package com.smartpresence.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartpresence.constants.StatutPresence;
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
 * Élément individuel de présence transmis par l'ESP32.
 *
 * <p>Représente fidèlement le payload minimal non biométrique :</p>
 * {@code studentId}, {@code deviceId}, {@code date}, {@code heure}, {@code statut}, {@code createdAt}.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceItemRequest {

    /**
     * Reference logique de l'empreinte, associee a l'etudiant lors de l'enrolement.
     *
     * <p>Le lecteur conserve en NVS la table {@code {slot AS608 -> biometricId}} et
     * n'a donc jamais a connaitre les identifiants internes du backend. Le champ
     * portait auparavant un UUID d'etudiant, ce qu'un lecteur ne peut pas obtenir :
     * aucun endpoint ne le lui fournit, et le transmettre contredirait le cloisonnement
     * applique au pointage du personnel.</p>
     *
     * <p><b>Aucune donnee biometrique n'est transmise</b> : le gabarit ne quitte jamais
     * le capteur.</p>
     */
    @NotBlank(message = "Le biometricId de l'etudiant est obligatoire")
    private String biometricId;

    @NotNull(message = "Le deviceId (UUID) est obligatoire")
    private UUID deviceId;

    @NotNull(message = "La date est obligatoire")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull(message = "L'heure est obligatoire")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heure;

    @NotNull(message = "Le statut est obligatoire")
    private StatutPresence statut;

    @NotNull(message = "Le createdAt du device (horodatage RTC) est obligatoire")
    private Instant createdAt;
}
