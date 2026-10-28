package com.smartpresence.dto.response;

import com.smartpresence.constants.StatutDocument;
import com.smartpresence.constants.StatutEmprunt;
import com.smartpresence.constants.TypeDocument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Réponses de la bibliothèque : documents numériques, livres physiques et emprunts. */
public final class BibliothequeResponses {

    private BibliothequeResponses() { }

    public record Document(
            UUID id, TypeDocument type, String titre, String auteur, String matiere,
            Long filiereId, String filiereLibelle, Integer annee, String description,
            String nomFichier, String typeMime, long taille, StatutDocument statut,
            String motifRefus, String deposePar, Instant deposeLe) { }

    public record Livre(
            UUID id, String titre, String auteur, String isbn, String categorie,
            Long filiereId, String filiereLibelle, String resume, String emplacement,
            int exemplaires, int disponibles) { }

    public record Emprunt(
            UUID id, UUID livreId, String livreTitre, String livreAuteur,
            UUID etudiantId, String etudiantNom, String etudiantMatricule,
            LocalDate emprunteLe, LocalDate retourPrevu, LocalDate retourneLe,
            StatutEmprunt statut, long joursDeRetard, int prolongations) { }

    /** Chiffres affichés en tête de la page d'administration. */
    public record Synthese(long documentsEnAttente, long empruntsEnCours, long empruntsEnRetard) { }
}
