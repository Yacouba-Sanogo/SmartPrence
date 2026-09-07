package com.smartpresence.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Données des paramètres de l'établissement exposées via l'API.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametreEtablissementResponse {

    private Long id;
    private String nom;
    private String sigle;
    private String email;
    private String telephone;
    private String fuseauHoraire;
    private int seuilRetardMinutes;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heureOuverture;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heureFermeture;
}
