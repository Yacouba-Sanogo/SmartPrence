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
    private Long filiereId;
    private String filiereLibelle;
    private Long optionId;
    private String optionLibelle;
    private Long classePrecedenteId;
    private String classePrecedenteCode;
    private int nombreEtudiants;

    /**
     * Enseignants rattachés.
     *
     * <p>Exposé parce qu'une classe à zéro enseignant est une anomalie silencieuse :
     * elle n'apparaît sur l'application mobile de personne, et rien dans la liste ne le
     * signalerait sans ce compteur.</p>
     */
    private int nombreEnseignants;

    /** Responsable de classe désigné par l'administrateur ; absent si personne ne l'est. */
    private java.util.UUID responsableId;
    private String responsableNom;
    private Instant createdAt;
    private Instant updatedAt;
}
