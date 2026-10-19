package com.smartpresence.service;

import com.smartpresence.dto.request.EnseignementRequests;
import com.smartpresence.dto.response.EnseignementResponses;
import com.smartpresence.dto.response.NoteResponse;

import java.util.List;
import java.util.UUID;

/**
 * Évaluations d'un enseignant : planification, barème, saisie des résultats avec absences,
 * puis transmission à l'administration.
 */
public interface EvaluationService {

    List<EnseignementResponses.Evaluation> evaluationsDeClasse(UUID utilisateurId, Long classeId, Long matiereId);

    EnseignementResponses.Evaluation planifier(UUID utilisateurId, EnseignementRequests.Evaluation requete);

    EnseignementResponses.Evaluation modifier(UUID utilisateurId, UUID evaluationId,
                                              EnseignementRequests.Evaluation requete);

    void supprimer(UUID utilisateurId, UUID evaluationId);

    EnseignementResponses.ResultatsEvaluation resultats(UUID utilisateurId, UUID evaluationId);

    /** Enregistre des résultats sur le barème de l'épreuve ; le serveur les ramène sur 20. */
    EnseignementResponses.ResultatsEvaluation enregistrerResultats(UUID utilisateurId, UUID evaluationId,
                                                                   EnseignementRequests.Resultats requete);

    /** Transmet à l'administration les notes en brouillon de l'évaluation. */
    List<NoteResponse> transmettre(UUID utilisateurId, UUID evaluationId);
}
