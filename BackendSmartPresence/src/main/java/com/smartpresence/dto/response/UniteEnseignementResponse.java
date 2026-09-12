package com.smartpresence.dto.response;

import com.smartpresence.constants.PeriodeScolaire;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Unité d'enseignement telle que la voit l'administration.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniteEnseignementResponse {

    private Long id;
    private String code;
    private String libelle;
    private int credits;
    private PeriodeScolaire semestre;

    /** « S1 » — l'étiquette courte. */
    private String semestreLibelle;

    private Long promotionId;
    private String promotionLibelle;
    private boolean active;

    /**
     * ECUE rattachés, pour vérifier d'un coup d'œil que la maquette est complète.
     *
     * <p>Réduits à l'essentiel : le catalogue complet des matières se consulte
     * ailleurs, et l'imbriquer ici alourdirait chaque lecture de la maquette.</p>
     */
    private List<EcueBrefResponse> ecues;

    /** Total des crédits des ECUE — doit correspondre à ceux de l'UE. */
    private int creditsEcues;

    /** Un ECUE, réduit à ce que la maquette a besoin d'afficher. */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EcueBrefResponse {
        private Long matiereId;
        private String code;
        private String libelle;
        private Integer credits;
        private boolean active;
    }
}
