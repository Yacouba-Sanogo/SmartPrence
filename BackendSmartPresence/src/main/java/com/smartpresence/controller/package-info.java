/**
 * Couche de présentation (API REST). Expose les endpoints HTTP, reçoit les
 * requêtes, délègue à la couche service et retourne des DTO.
 *
 * <p><b>Règle absolue :</b> aucune logique métier ne doit s'y trouver.
 * Son rôle se limite à l'orchestration HTTP.</p>
 *
 * @since 0.0.1
 * @see SmartPresence_CONTEXT.md §6.3
 */
package com.smartpresence.controller;
