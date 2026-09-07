package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Données résumées d'une classe exposées via l'API.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClasseResponse {

    private Long id;
    private String code;
    private String libelle;
    private PromotionResponse promotion;
    private int nombreEtudiants;

    /**
     * Enseignants rattachés.
     *
     * <p>Exposé parce qu'une classe à zéro enseignant est une anomalie silencieuse :
     * elle n'apparaît sur l'application mobile de personne, et rien dans la liste ne le
     * signalerait sans ce compteur.</p>
     */
    private int nombreEnseignants;
    private Instant createdAt;
    private Instant updatedAt;
}
