package com.smartpresence.service;

import com.smartpresence.dto.request.InscriptionEtudiantRequest;
import com.smartpresence.dto.response.ClasseResponse;
import com.smartpresence.dto.response.EtudiantResponse;

import java.util.List;

/**
 * Inscription des étudiants par eux-mêmes.
 *
 * <p>Saisir une promotion entière incombait jusqu'ici à l'administration. L'étudiant
 * dépose désormais lui-même son dossier ; le contrôle ne porte plus sur qui saisit,
 * mais sur le numéro CENOU présenté, qui doit figurer au référentiel et n'avoir
 * jamais servi.</p>
 */
public interface InscriptionService {

    /**
     * Classes proposées au formulaire d'inscription.
     *
     * <p>Exposée <b>sans authentification</b> : le candidat doit pouvoir choisir sa
     * classe avant d'avoir le moindre compte. Elle ne divulgue que des libellés
     * d'organisation, aucune donnée personnelle.</p>
     */
    List<ClasseResponse> classesOuvertes();

    /**
     * Enregistre une inscription et ouvre l'accès mobile dans le même geste.
     *
     * @throws com.smartpresence.exception.BusinessException si le numéro CENOU est
     *         inconnu du référentiel ou a déjà servi
     */
    EtudiantResponse inscrire(InscriptionEtudiantRequest request);
}
