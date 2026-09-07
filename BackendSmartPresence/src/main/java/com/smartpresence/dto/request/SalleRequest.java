package com.smartpresence.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Requête de création ou modification d'une salle.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalleRequest {

    @NotBlank(message = "Le code de la salle est obligatoire")
    private String code;

    @NotBlank(message = "Le nom de la salle est obligatoire")
    private String nom;

    private String batiment;
    private Integer capacite;
}
