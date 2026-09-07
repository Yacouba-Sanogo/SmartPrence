package com.smartpresence.service;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.dto.request.NoteRequest;
import com.smartpresence.dto.response.BulletinResponse;
import com.smartpresence.dto.response.NoteResponse;

import java.util.List;
import java.util.UUID;

/**
 * Notes et bulletins.
 *
 * @since 0.0.1
 */
public interface NoteService {

    /**
     * Bulletin de l'étudiant connecté.
     *
     * @param periode période observée ; {@code null} pour toute l'année
     */
    BulletinResponse monBulletin(UUID utilisateurId, PeriodeScolaire periode);

    /** Notes d'une classe où l'enseignant connecté intervient. */
    List<NoteResponse> notesDeMaClasse(UUID utilisateurId, Long classeId,
                                       Long matiereId, PeriodeScolaire periode);

    /**
     * Saisit une note.
     *
     * @throws com.smartpresence.exception.BusinessException si l'enseignant n'intervient
     *         pas dans la classe de l'étudiant
     */
    NoteResponse saisir(UUID utilisateurId, NoteRequest request);

    /**
     * Modifie une note.
     *
     * @throws com.smartpresence.exception.BusinessException si la note a été saisie par
     *         un autre enseignant
     */
    NoteResponse modifier(UUID utilisateurId, UUID noteId, NoteRequest request);

    /** Supprime une note dont l'enseignant connecté est l'auteur. */
    void supprimer(UUID utilisateurId, UUID noteId);

    /** Bulletin d'un étudiant, vu par l'administration. */
    BulletinResponse bulletinDe(UUID etudiantId, PeriodeScolaire periode);
}
