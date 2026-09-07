package com.smartpresence.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Arbitrage d'un signalement par la scolarité.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraitementSignalementRequest {

    /** {@code true} pour retenir le signalement, {@code false} pour l'écarter. */
    @NotNull(message = "La décision est obligatoire")
    private Boolean accepte;

    /**
     * Motif de la décision — <b>obligatoire dans les deux sens</b>.
     *
     * <p>Justifier un refus va de soi ; justifier une acceptation l'est tout autant,
     * puisqu'elle produit un relevé qui n'a pas été constaté par le capteur.</p>
     */
    @NotBlank(message = "Le motif de la décision est obligatoire")
    @Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères")
    private String commentaire;

    /**
     * Heure à porter sur le relevé correctif, pour un {@code ETUDIANT_NON_RECONNU}
     * accepté.
     *
     * <p>Facultative : à défaut, l'heure de début de la séance est retenue.</p>
     */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heurePresence;
}
