package com.smartpresence.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Résultat d'une capture, renvoyé par le lecteur au serveur.
 *
 * <p>Le gabarit d'empreinte reste dans la mémoire du capteur AS608 : seule sa
 * place y est communiquée, sous la forme d'une référence logique du type
 * {@code ETU-0042}.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceEnrolementRequest {

    /** Code de la demande servie, tel qu'il était affiché à l'écran. */
    @NotBlank(message = "Le code de la demande est obligatoire")
    @Size(max = 10, message = "Le code ne peut pas dépasser 10 caractères")
    private String code;

    /** Référence logique de l'emplacement occupé dans le capteur. */
    @NotBlank(message = "La référence biométrique est obligatoire")
    @Size(max = 100, message = "La référence ne peut pas dépasser 100 caractères")
    private String reference;
}
