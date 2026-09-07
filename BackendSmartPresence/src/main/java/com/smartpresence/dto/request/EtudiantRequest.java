package com.smartpresence.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Requête de création ou modification d'un étudiant.
 *
 * <p>Contient {@code biometricId} qui est un identifiant de correspondance logique,
 * <b>jamais une empreinte digitale</b>.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtudiantRequest {

    @NotBlank(message = "Le matricule est obligatoire")
    private String matricule;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @Email(message = "Format d'email invalide")
    private String email;

    private String telephone;
    private LocalDate dateNaissance;

    /**
     * Référence biométrique, <b>facultative</b> : l'enrôlement est un acte distinct,
     * tracé par son propre endpoint.
     */
    private String biometricId;

    @NotNull(message = "L'identifiant de la classe est obligatoire")
    private Long classeId;

    private Boolean actif;
}
