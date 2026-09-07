package com.smartpresence.dto.response;

import com.smartpresence.constants.TypePersonnel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Données d'un membre du personnel exposées via l'API REST.
 *
 * <p>{@code biometricId} n'est qu'une <b>référence logique</b> de correspondance : il ne
 * contient aucune empreinte ni gabarit biométrique (cf. {@code SmartPresence_CONTEXT.md} §13).</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonnelResponse {

    private UUID id;
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private TypePersonnel type;
    private String service;
    private boolean actif;

    /** Référence logique de correspondance biométrique, {@code null} si non enrôlé. */
    private String biometricId;

    /** {@code true} si l'agent dispose d'une empreinte enrôlée et peut donc pointer. */
    private boolean enrole;
}
