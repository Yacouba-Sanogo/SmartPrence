package com.smartpresence.service;

import com.smartpresence.dto.response.GestionResponses;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** Supports de cours : publiés par l'enseignant, consultés par les étudiants de la classe. */
public interface SupportCoursService {

    /** Contenu d'un support prêt à être servi. */
    record Fichier(String nom, String typeMime, byte[] octets) { }

    GestionResponses.Support publier(UUID utilisateurId, Long classeId, Long matiereId, String titre,
                                     String description, MultipartFile fichier);

    /** Supports que l'enseignant a publiés dans une de ses classes. */
    List<GestionResponses.Support> mesSupports(UUID utilisateurId, Long classeId);

    /** Supports de la classe de l'étudiant connecté. */
    List<GestionResponses.Support> supportsDeMaClasse(UUID utilisateurId);

    void supprimer(UUID utilisateurId, UUID supportId);

    /** Auteur du support, ou étudiant de sa classe : personne d'autre. */
    Fichier telecharger(UUID utilisateurId, UUID supportId);
}
