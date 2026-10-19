package com.smartpresence.service;

import com.smartpresence.dto.request.EnseignementRequests;
import com.smartpresence.dto.response.EnseignementResponses;
import com.smartpresence.dto.response.JourneePersonnelResponse;
import com.smartpresence.dto.response.SeanceResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Ce que fait l'enseignant de son cours, au-delà de la feuille de présence : déclarer ses
 * séances, tenir son cahier de textes et son syllabus, suivre son propre pointage.
 */
public interface EnseignementService {

    /** Ses propres pointages (arrivées, départs) sur une période. */
    List<JourneePersonnelResponse> mesPointages(UUID utilisateurId, LocalDate debut, LocalDate fin);

    /**
     * Déclare une séance effectuée. Refusée si elle est annulée ou pas encore commencée ;
     * idempotente, pour qu'un second appui (réseau lent) ne change rien.
     */
    SeanceResponse declarerSeance(UUID utilisateurId, UUID seanceId);

    EnseignementResponses.Bilan bilan(UUID utilisateurId, LocalDate debut, LocalDate fin);

    // Cahier de textes -------------------------------------------------

    EnseignementResponses.PageCahier cahierDeSeance(UUID utilisateurId, UUID seanceId);

    EnseignementResponses.PageCahier ecrireCahier(UUID utilisateurId, UUID seanceId,
                                                 EnseignementRequests.Cahier requete);

    List<EnseignementResponses.PageCahier> cahierDeClasse(UUID utilisateurId, Long classeId, Long matiereId);

    // Syllabus ---------------------------------------------------------

    List<EnseignementResponses.Syllabus> syllabus(UUID utilisateurId, Long classeId, Long matiereId);

    EnseignementResponses.Chapitre ajouterChapitre(UUID utilisateurId, EnseignementRequests.NouveauChapitre requete);

    EnseignementResponses.Chapitre modifierChapitre(UUID utilisateurId, UUID chapitreId,
                                                   EnseignementRequests.ModificationChapitre requete);

    void supprimerChapitre(UUID utilisateurId, UUID chapitreId);
}
