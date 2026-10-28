package com.smartpresence.service;

import com.smartpresence.constants.TypeDocument;
import com.smartpresence.dto.request.BibliothequeRequests;
import com.smartpresence.dto.response.BibliothequeResponses;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Bibliothèque : mémoires et anciens sujets numériques, fonds de livres physiques, emprunts.
 *
 * <p>{@code gestionnaire} vaut {@code true} pour l'administration (ADMIN, RESPONSABLE_SCOLARITE) :
 * ses dépôts sont publiés d'emblée et elle voit tous les documents.</p>
 */
public interface BibliothequeService {

    record Fichier(String nom, String typeMime, byte[] octets) { }

    // ----- Documents numériques -----------------------------------------

    List<BibliothequeResponses.Document> documents(TypeDocument type, Long filiereId, Integer annee, String recherche);

    List<BibliothequeResponses.Document> documentsAModerer();

    List<BibliothequeResponses.Document> mesDepots(UUID utilisateurId);

    BibliothequeResponses.Document deposer(UUID utilisateurId, boolean gestionnaire, TypeDocument type, String titre,
                                           String auteur, String matiere, Long filiereId, Integer annee,
                                           String description, MultipartFile fichier);

    BibliothequeResponses.Document decider(UUID documentId, BibliothequeRequests.Decision decision);

    void retirer(UUID utilisateurId, boolean gestionnaire, UUID documentId);

    Fichier telecharger(UUID utilisateurId, boolean gestionnaire, UUID documentId);

    // ----- Livres ---------------------------------------------------------

    List<BibliothequeResponses.Livre> livres(Long filiereId, String recherche);

    BibliothequeResponses.Livre creerLivre(BibliothequeRequests.Livre requete);

    BibliothequeResponses.Livre modifierLivre(UUID id, BibliothequeRequests.Livre requete);

    void supprimerLivre(UUID id);

    // ----- Emprunts -------------------------------------------------------

    List<BibliothequeResponses.Emprunt> emprunts();

    List<BibliothequeResponses.Emprunt> mesEmprunts(UUID utilisateurId);

    BibliothequeResponses.Emprunt emprunter(BibliothequeRequests.Emprunt requete);

    BibliothequeResponses.Emprunt retourner(UUID empruntId);

    BibliothequeResponses.Emprunt prolonger(UUID empruntId, int jours);

    BibliothequeResponses.Synthese synthese();

    /** Rappels de retour et de retard aux étudiants concernés ; renvoie le nombre de rappels envoyés. */
    int envoyerRappels();
}
