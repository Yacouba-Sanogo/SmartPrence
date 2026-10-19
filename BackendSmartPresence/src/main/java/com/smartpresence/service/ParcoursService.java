package com.smartpresence.service;

import com.smartpresence.dto.response.ChoixOptionResponse;
import com.smartpresence.dto.response.EtudiantResponse;
import com.smartpresence.dto.response.PassageResponse;

import java.util.UUID;

/**
 * Parcours d'un étudiant d'une année à l'autre : changement de classe, choix d'option
 * et passage en année supérieure.
 */
public interface ParcoursService {

    /** Déplace un étudiant vers une autre classe (correction d'affectation, transfert). */
    EtudiantResponse changerClasse(UUID etudiantId, Long classeId);

    /** Options proposées par la filière de la classe de l'étudiant, et celle qu'il a retenue. */
    ChoixOptionResponse options(UUID etudiantId);

    /** Enregistre l'option retenue ; elle doit appartenir à la filière de sa classe. */
    ChoixOptionResponse choisirOption(UUID etudiantId, Long optionId);

    /**
     * Fait passer en année supérieure les étudiants admis.
     *
     * <p>Admis : tous les semestres de la maquette sont validés. Un semestre non validé
     * donne un redoublant ; une note manquante laisse l'étudiant en attente, sans rien
     * décider à sa place. Toutes les décisions sont prises avant le premier
     * déplacement, de sorte qu'un étudiant ne monte jamais de deux classes d'un coup.</p>
     *
     * @param classeId {@code null} pour toutes les classes
     * @param appliquer {@code false} pour simuler sans rien déplacer
     */
    PassageResponse passage(Long classeId, boolean appliquer);
}
