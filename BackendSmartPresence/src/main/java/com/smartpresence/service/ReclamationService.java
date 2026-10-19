package com.smartpresence.service;

import com.smartpresence.dto.request.GestionRequests;
import com.smartpresence.dto.response.GestionResponses;

import java.util.List;
import java.util.UUID;

/** Contestation des notes par les étudiants, traitée par l'enseignant auteur. */
public interface ReclamationService {

    /** Réservé aux notes publiées : un brouillon n'est pas visible de l'étudiant. */
    GestionResponses.Reclamation contester(UUID utilisateurId, UUID noteId, GestionRequests.Contestation requete);

    List<GestionResponses.Reclamation> mesContestations(UUID utilisateurId);

    List<GestionResponses.Reclamation> recues(UUID utilisateurId);

    GestionResponses.Reclamation repondre(UUID utilisateurId, UUID reclamationId,
                                          GestionRequests.ReponseReclamation reponse);
}
