package com.smartpresence.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Liste d'étudiants à inscrire d'un coup dans une classe.
 *
 * <p>Le fichier (CSV, Excel ou photo d'une liste) est lu côté administration : le serveur ne
 * reçoit que des lignes déjà structurées, que l'administrateur a pu corriger à l'écran.
 * {@code simulation} vérifie tout sans rien enregistrer.</p>
 */
public record ImportEtudiantsRequest(
        @NotNull(message = "La classe est obligatoire") Long classeId,
        boolean simulation,
        @NotEmpty(message = "Aucun étudiant à importer")
        @Size(max = 500, message = "500 étudiants au plus par import")
        @Valid List<Ligne> etudiants) {

    public record Ligne(
            String matricule,
            String nom,
            String prenom,
            String email,
            String telephone,
            LocalDate dateNaissance) { }
}
