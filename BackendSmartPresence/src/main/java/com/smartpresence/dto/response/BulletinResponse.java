package com.smartpresence.dto.response;

import com.smartpresence.constants.PeriodeScolaire;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Bulletin d'un étudiant sur une période.
 *
 * <h2>Les moyennes sont calculées par le serveur</h2>
 * <p>Elles pourraient l'être par le client à partir des notes brutes. Ce serait une
 * erreur : trois clients — le mobile étudiant, l'administration web, un futur export —
 * les recalculeraient chacun de leur côté, et la moindre divergence d'arrondi produirait
 * deux bulletins différents pour le même étudiant. Un bulletin doit avoir une seule
 * valeur, celle du serveur.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulletinResponse {

    private UUID etudiantId;
    private String etudiantNom;
    private String etudiantPrenom;
    private String etudiantMatricule;
    private String classeCode;
    private String classeLibelle;

    private PeriodeScolaire periode;

    /**
     * Moyenne générale sur 20, {@code null} si aucune note n'a encore été saisie.
     *
     * <p>{@code null} et non zéro : un étudiant sans note n'a pas 0 de moyenne, il n'a
     * pas de moyenne. Les confondre afficherait un échec là où il n'y a qu'une absence
     * d'évaluation.</p>
     */
    private BigDecimal moyenneGenerale;

    /** Nombre total de notes prises en compte. */
    private int nombreNotes;

    private List<LigneMatiereResponse> matieres;

    /** Résultat d'un étudiant dans une matière. */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LigneMatiereResponse {

        private Long matiereId;
        private String matiereCode;
        private String matiereLibelle;

        /**
         * Poids de la matière dans la moyenne générale.
         *
         * <p>Repris des crédits de la matière quand ils existent — une matière à 5
         * crédits pèse cinq fois une matière à 1. À défaut, 1.</p>
         */
        private int coefficient;

        /** Moyenne pondérée des notes de la matière. */
        private BigDecimal moyenne;

        private List<NoteResponse> notes;
    }
}
