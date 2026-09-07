package com.smartpresence.service;

import com.smartpresence.constants.TypePersonnel;
import com.smartpresence.dto.request.EnrolementBiometriqueRequest;
import com.smartpresence.dto.request.PersonnelRequest;
import com.smartpresence.dto.response.ComptePersonnelResponse;
import com.smartpresence.dto.response.PersonnelResponse;

import java.util.List;
import java.util.UUID;

/**
 * Contrat métier du référentiel du personnel et de son enrôlement biométrique.
 *
 * @since 0.0.1
 */
public interface PersonnelService {

    /**
     * Liste les agents, filtrables par catégorie et par état d'activité.
     *
     * @param type       catégorie recherchée, ou {@code null} pour toutes
     * @param actifsSeul {@code true} pour ne retenir que les agents actifs
     * @return agents triés par nom puis prénom
     */
    List<PersonnelResponse> findAll(TypePersonnel type, boolean actifsSeul);

    PersonnelResponse findById(UUID id);

    PersonnelResponse create(PersonnelRequest request);

    PersonnelResponse update(UUID id, PersonnelRequest request);

    /**
     * Active ou désactive un agent. Un agent inactif ne peut plus pointer.
     *
     * @param id    agent concerné
     * @param actif nouvel état
     */
    PersonnelResponse setActif(UUID id, boolean actif);

    /**
     * Associe à l'agent la référence logique de l'empreinte enrôlée sur le capteur.
     *
     * <p>Aucune donnée biométrique n'est reçue ni stockée : seule la référence de
     * correspondance est enregistrée (cf. {@code SmartPresence_CONTEXT.md} §13).</p>
     *
     * @param id      agent à enrôler
     * @param request référence biométrique et lecteur d'enrôlement
     * @return l'agent enrôlé
     */
    PersonnelResponse enroler(UUID id, EnrolementBiometriqueRequest request);

    /**
     * Révoque l'enrôlement biométrique d'un agent (départ, empreinte à réenrôler).
     *
     * <p>Les pointages déjà enregistrés sont conservés : ils constituent un historique
     * immuable. Seule la capacité à pointer est retirée.</p>
     *
     * @param id agent concerné
     */
    PersonnelResponse revoquerEnrolement(UUID id);

    /**
     * Ouvre un accès applicatif à l'agent : crée son compte et lui attribue le rôle
     * correspondant à sa catégorie.
     *
     * <p>Un enseignant reçoit {@code ENSEIGNANT}, qui lui ouvre ses classes et ses
     * séances ; tout autre agent reçoit {@code PERSONNEL}, limité à la consultation de
     * ses propres pointages. Le mot de passe est tiré au hasard et retourné
     * <b>une seule fois</b>.</p>
     *
     * @param id agent concerné
     * @return les identifiants à remettre à l'agent
     */
    ComptePersonnelResponse ouvrirCompte(UUID id);

    /**
     * Ferme l'accès applicatif d'un agent et supprime son compte.
     *
     * <p>Les pointages déjà relevés sont conservés : ils relèvent du dossier horaire,
     * pas du compte de consultation.</p>
     */
    PersonnelResponse fermerCompte(UUID id);
}
