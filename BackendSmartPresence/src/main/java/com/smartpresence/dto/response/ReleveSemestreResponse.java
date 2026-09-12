package com.smartpresence.dto.response;

import com.smartpresence.constants.DecisionSemestre;
import com.smartpresence.constants.PeriodeScolaire;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Relevé de notes d'un semestre, au format LMD.
 *
 * <p>Trois niveaux de lecture, du plus synthétique au plus détaillé : la décision et
 * les crédits en tête, les UE ensuite, et sous chaque UE le détail de ses ECUE avec
 * la note de devoir et celle d'examen. C'est la structure du relevé que l'étudiant
 * reçoit sur papier — l'écran ne fait que la reprendre.</p>
 *
 * <p><b>Tout est calculé par le serveur.</b> Un recalcul côté application produirait,
 * au moindre écart d'arrondi, un relevé différent de celui de l'administration.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleveSemestreResponse {

    private String etudiantNom;
    private String etudiantPrenom;
    private String etudiantMatricule;
    private String classeCode;
    private String classeLibelle;
    private String promotionLibelle;

    private PeriodeScolaire semestre;

    /** « S1 » — l'étiquette de l'onglet. */
    private String semestreLibelle;

    /** Semestres où la promotion a une maquette : les onglets à proposer, et eux seuls. */
    private List<PeriodeScolaire> semestresDisponibles;

    /** Moyenne du semestre, pondérée par les crédits des UE. {@code null} sans note. */
    private BigDecimal moyenneGenerale;

    private int creditsAcquis;

    /** Total des crédits de la maquette du semestre — 30 en général. */
    private int creditsRequis;

    private DecisionSemestre decision;

    /**
     * Vrai lorsque des UE sous la barre ont été acquises par la moyenne générale.
     *
     * <p>Affiché à l'étudiant : une UE à 9 marquée « acquise » sans explication est
     * incompréhensible.</p>
     */
    private boolean compensationAppliquee;

    private int nombreNotes;

    private List<UniteResponse> unites;

    /** Résultat dans une unité d'enseignement. */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UniteResponse {

        private Long uniteId;
        private String code;
        private String libelle;

        /** Crédits que l'UE rapporte si elle est acquise. */
        private int credits;

        /** Crédits effectivement obtenus : {@link #credits} ou zéro. */
        private int creditsAcquis;

        /** Moyenne de l'UE, pondérée par les crédits de ses ECUE. */
        private BigDecimal moyenne;

        private boolean acquise;

        /** Vrai si l'UE n'est acquise que par la compensation du semestre. */
        private boolean acquiseParCompensation;

        private List<EcueResponse> ecues;
    }

    /** Résultat dans un élément constitutif — une matière. */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EcueResponse {

        private Long matiereId;
        private String code;
        private String libelle;
        private int credits;

        /** Moyenne des évaluations de contrôle continu. {@code null} si aucune. */
        private BigDecimal noteDevoir;

        /** Moyenne des examens. {@code null} si l'examen n'a pas encore eu lieu. */
        private BigDecimal noteExamen;

        /** Devoir et examen combinés selon la pondération du règlement. */
        private BigDecimal moyenne;

        /** Détail des évaluations, pour qui veut savoir d'où sort la moyenne. */
        private List<NoteResponse> notes;
    }
}
