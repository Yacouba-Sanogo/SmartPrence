package com.smartpresence.dto.response;

import java.util.List;

/** Bilan d'un import d'étudiants, ligne par ligne. */
public record ImportEtudiantsResponse(
        boolean simulation, int creees, int ignorees, int erreurs, List<Ligne> lignes) {

    /** {@code statut} : CREE, IGNORE (déjà inscrit) ou ERREUR ; en simulation, CREE veut dire « serait créé ». */
    public record Ligne(int indice, String matricule, String nom, String prenom, String statut, String message) { }
}
