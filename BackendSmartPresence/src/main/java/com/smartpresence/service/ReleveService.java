package com.smartpresence.service;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.dto.response.ReleveSemestreResponse;

import java.util.UUID;

/**
 * Relevé de notes au format LMD : UE, ECUE, crédits et décision.
 *
 * <h2>Pourquoi à côté du bulletin, et non à sa place</h2>
 * <p>Le bulletin existant lit les notes matière par matière — c'est la vue de
 * l'enseignant, et elle reste juste. Le relevé lit les mêmes notes à travers la
 * maquette pédagogique : les matières deviennent des ECUE, se regroupent en UE, et
 * ce sont les UE qui portent les crédits. Une matière non rattachée à une UE reste
 * notable et figure au bulletin ; elle n'apparaît simplement pas ici.</p>
 *
 * @since 0.0.1
 */
public interface ReleveService {

    /**
     * Relevé de l'étudiant connecté.
     *
     * @param semestre semestre demandé ; à défaut, le premier de la maquette
     */
    ReleveSemestreResponse monReleve(UUID utilisateurId, PeriodeScolaire semestre);

    /** Relevé d'un étudiant, pour la scolarité. */
    ReleveSemestreResponse releveDe(UUID etudiantId, PeriodeScolaire semestre);
}
