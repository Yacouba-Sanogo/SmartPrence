package com.smartpresence.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Demande d'inscription déposée par l'étudiant lui-même.
 *
 * <p>Requête <b>publique</b> : elle n'exige aucun jeton. Son seul verrou est le
 * numéro CENOU, qui doit figurer au référentiel et n'avoir jamais servi.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InscriptionEtudiantRequest {

    @NotBlank(message = "Le numéro CENOU est obligatoire")
    @Size(max = 50, message = "Le numéro CENOU ne peut pas dépasser 50 caractères")
    private String numeroCenou;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    private String prenom;

    /**
     * Adresse personnelle, facultative.
     *
     * <p>Absente, l'identifiant de connexion est fabriqué à partir du numéro CENOU :
     * tout étudiant peut donc s'inscrire sans posséder d'adresse électronique.</p>
     */
    @Email(message = "Format d'email invalide")
    @Size(max = 150, message = "L'email ne peut pas dépasser 150 caractères")
    private String email;

    @Size(max = 20, message = "Le téléphone ne peut pas dépasser 20 caractères")
    private String telephone;

    @Past(message = "La date de naissance doit être passée")
    private LocalDate dateNaissance;

    /**
     * Classe choisie par l'étudiant.
     *
     * <p>Ignorée lorsque le référentiel impose déjà une classe à ce numéro —
     * l'affectation relève alors de l'administration, pas de l'intéressé.</p>
     */
    private Long classeId;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 100, message = "Le mot de passe doit compter au moins 8 caractères")
    private String motDePasse;
}
