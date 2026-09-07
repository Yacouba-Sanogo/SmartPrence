package com.smartpresence.constants;

/**
 * Nature d'une évaluation.
 *
 * <p>Le poids réel d'une note vient de son coefficient, pas de son type : deux devoirs
 * peuvent peser différemment. Le type sert à l'affichage et au classement, jamais au
 * calcul.</p>
 *
 * @since 0.0.1
 */
public enum TypeEvaluation {
    DEVOIR,
    INTERROGATION,
    TRAVAUX_PRATIQUES,
    COMPOSITION,
    EXAMEN
}
