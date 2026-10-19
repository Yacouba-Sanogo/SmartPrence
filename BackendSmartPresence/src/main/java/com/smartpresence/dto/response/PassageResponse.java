package com.smartpresence.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Compte rendu d'un passage en année supérieure, simulé ou appliqué.
 *
 * @param appliquee {@code false} pour une simulation : rien n'a été déplacé
 */
public record PassageResponse(boolean appliquee, int promus, int redoublants, int enAttente,
                              int termines, int optionsManquantes, List<Ligne> lignes) {

    /** Issue de l'année pour un étudiant. */
    public enum Issue {
        /** Admis : rejoint la classe supérieure. */
        PROMU,
        /** Un semestre non validé : reste dans sa classe. */
        REDOUBLANT,
        /** Notes ou semestres manquants : aucune décision possible, il ne bouge pas. */
        EN_ATTENTE,
        /** Admis, mais aucune classe supérieure n'est définie : fin de cursus ou configuration à faire. */
        TERMINE,
        /** Admis, la filière se divise en options et l'étudiant n'a pas choisi la sienne. */
        OPTION_A_CHOISIR
    }

    /** Une ligne par étudiant. */
    public record Ligne(UUID etudiantId, String matricule, String nomComplet,
                        String classeOrigine, String classeDestination, Issue issue, String detail) {
    }
}
