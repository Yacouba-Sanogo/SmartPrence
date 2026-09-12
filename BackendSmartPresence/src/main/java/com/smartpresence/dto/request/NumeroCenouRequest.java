package com.smartpresence.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Requête d'ajout d'un numéro CENOU au référentiel des inscriptions autorisées.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NumeroCenouRequest {

    @NotBlank(message = "Le numéro CENOU est obligatoire")
    @Size(max = 50, message = "Le numéro CENOU ne peut pas dépasser 50 caractères")
    private String numero;

    /** Facultatifs : ils servent à pré-remplir le formulaire d'inscription. */
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String nom;

    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    private String prenom;

    /** Classe imposée. Laissée vide, l'étudiant choisira la sienne. */
    private Long classeId;
}
