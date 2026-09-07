package com.smartpresence.service;

import com.smartpresence.dto.response.ClasseResponse;
import com.smartpresence.dto.response.EffectifEtudiantResponse;
import com.smartpresence.dto.response.FeuilleSeanceResponse;
import com.smartpresence.dto.response.JustificationAbsenceResponse;
import com.smartpresence.dto.response.PagedResponse;
import com.smartpresence.dto.response.PresenceResponse;
import com.smartpresence.dto.response.ProfilResponse;
import com.smartpresence.dto.response.SeanceResponse;
import com.smartpresence.dto.response.StatistiquesEtudiantResponse;
import com.smartpresence.entity.Etudiant;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Accès de l'utilisateur connecté à <b>ses propres</b> données.
 *
 * <p>Aucune méthode ne prend d'identifiant de personne : le sujet est toujours déduit du
 * jeton. C'est ce qui rend l'accès aux données d'autrui impossible par construction,
 * plutôt que par une vérification qu'on pourrait oublier d'écrire.</p>
 *
 * @since 0.0.1
 */
public interface ProfilService {

    /**
     * Résout l'identité complète de l'utilisateur connecté.
     *
     * @param utilisateurId identifiant issu du jeton
     */
    ProfilResponse profil(UUID utilisateurId);

    /**
     * Présences de l'étudiant connecté sur une période.
     *
     * @throws com.smartpresence.exception.BusinessException si le compte n'est rattaché
     *         à aucune fiche étudiant
     */
    PagedResponse<PresenceResponse> mesPresences(UUID utilisateurId, LocalDate debut, LocalDate fin,
                                                 int page, int taille);

    /** Statistiques d'assiduité de l'étudiant connecté. */
    StatistiquesEtudiantResponse mesStatistiques(UUID utilisateurId);

    /** Justificatifs d'absence déposés par l'étudiant connecté. */
    List<JustificationAbsenceResponse> mesJustificatifs(UUID utilisateurId);

    // ------------------------------------------------------------------
    // Espace enseignant
    // ------------------------------------------------------------------

    /**
     * Classes dans lesquelles l'enseignant connecté intervient.
     *
     * @throws com.smartpresence.exception.BusinessException si le compte n'est rattaché
     *         à aucune fiche d'agent de catégorie enseignant
     */
    List<ClasseResponse> mesClasses(UUID utilisateurId);

    /**
     * Séances assurées par l'enseignant connecté sur une journée.
     *
     * @param jour journée observée
     */
    List<SeanceResponse> mesSeances(UUID utilisateurId, LocalDate jour);

    /**
     * Effectif nominatif d'une classe où l'enseignant intervient.
     *
     * <p>L'appartenance est vérifiée : le rattachement à la classe est la seule chose qui
     * autorise à en connaître les inscrits. Sans cette route, un enseignant devait passer
     * par {@code GET /etudiants}, qui lui donnait l'école entière.</p>
     *
     * @param classeId classe consultée
     */
    List<EffectifEtudiantResponse> mesEtudiants(UUID utilisateurId, Long classeId);

    /**
     * Feuille de présence d'une séance que l'enseignant assure.
     *
     * <p>L'appartenance est vérifiée : un enseignant ne consulte pas la feuille d'un
     * cours qui n'est pas le sien.</p>
     */
    FeuilleSeanceResponse feuilleDeSeance(UUID utilisateurId, UUID seanceId);

    /**
     * Fiche étudiant rattachée à un compte.
     *
     * <p>Exposée pour permettre aux autres services de vérifier qu'un appelant agit bien
     * sur son propre dossier.</p>
     */
    Etudiant etudiantDuCompte(UUID utilisateurId);
}
