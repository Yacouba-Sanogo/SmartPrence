package com.smartpresence.service;

import com.smartpresence.constants.StatutDemande;
import com.smartpresence.dto.request.GestionRequests;
import com.smartpresence.dto.response.GestionResponses;

import java.util.List;
import java.util.UUID;

/** Demandes de congé : soumises par l'enseignant, décidées par l'administration. */
public interface CongeService {

    List<GestionResponses.Conge> mesDemandes(UUID utilisateurId);

    /** Refuse un chevauchement avec une demande en attente ou acceptée. */
    GestionResponses.Conge demander(UUID utilisateurId, GestionRequests.Conge requete);

    /** Retire une demande encore en attente. */
    GestionResponses.Conge annuler(UUID utilisateurId, UUID demandeId);

    List<GestionResponses.Conge> aTraiter(StatutDemande statut);

    /** Décide une demande en attente et prévient l'enseignant. */
    GestionResponses.Conge decider(UUID demandeId, GestionRequests.DecisionConge decision);
}
