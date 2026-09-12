package com.smartpresence.constants;

/**
 * Période à laquelle se rattache une note.
 *
 * <p>Sépare les moyennes d'un semestre à l'autre. Sans elle, un bulletin mélangerait
 * toute l'année et ne dirait plus rien de la progression.</p>
 *
 * <p>Les six semestres couvrent le cycle licence entier : un étudiant de troisième
 * année doit pouvoir relire ses résultats de première, et le relevé se consulte
 * semestre par semestre, comme le veut le LMD.</p>
 *
 * @since 0.0.1
 */
public enum PeriodeScolaire {
    SEMESTRE_1,
    SEMESTRE_2,
    SEMESTRE_3,
    SEMESTRE_4,
    SEMESTRE_5,
    SEMESTRE_6;

    /** « S1 » — l'étiquette courte des onglets de relevé. */
    public String libelleCourt() {
        return "S" + rang();
    }

    /** 1 à 6, pour ordonner les semestres sans dépendre de l'ordre de déclaration. */
    public int rang() {
        return ordinal() + 1;
    }
}
