package com.smartpresence.constants;

/**
 * Codes de rôles applicatifs du système SmartPresence.
 *
 * <p>Ces codes identifient les responsabilités attribuées aux utilisateurs dans le
 * cadre du contrôle d'accès basé sur les rôles (RBAC multi-rôles). Un utilisateur
 * peut posséder plusieurs rôles simultanément (relation {@code Utilisateur ↔ Set<Role>}).</p>
 *
 * <p>Voir {@code SmartPresence_CONTEXT.md} §4.11 et §9.2.</p>
 *
 * @since 0.0.1
 */
public enum RoleCode {

    /** Administrateur : accès complet au système, gestion de la configuration et des utilisateurs. */
    ADMIN,

    /** Enseignant : consultation et gestion des présences de ses classes. */
    ENSEIGNANT,

    /** Responsable de scolarité : gestion administrative des étudiants, des promotions et justifications. */
    RESPONSABLE_SCOLARITE,

    /** Superviseur : supervision globale, indicateurs et rapports transverses. */
    SUPERVISEUR,

    /**
     * Ressources humaines : gestion du référentiel personnel, enrôlement biométrique
     * et exploitation des pointages d'arrivée/départ.
     */
    RH,

    /**
     * Membre du personnel : consultation de <b>ses propres</b> pointages et de son
     * cumul d'heures. Aucun accès au référentiel des autres agents.
     */
    PERSONNEL,

    /**
     * Étudiant : consultation de <b>ses propres</b> présences, de son emploi du temps
     * et de ses justificatifs, depuis l'application mobile.
     *
     * <p>Rôle le plus restreint du système : il n'ouvre aucun accès au référentiel ni
     * aux données d'un autre étudiant.</p>
     */
    ETUDIANT

}
