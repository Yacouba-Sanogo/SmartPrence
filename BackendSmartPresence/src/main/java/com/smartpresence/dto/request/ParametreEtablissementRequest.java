package com.smartpresence.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametreEtablissementRequest {

    private String code;
    private String nom;
    private String sigle;
    private String email;
    private String telephone;
    private String fuseauHoraire;
    private Integer seuilRetardMinutes;

    /** Heure de prise de service du personnel, format {@code HH:mm:ss}. */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heureOuverture;

    /** Heure de fin de service du personnel, format {@code HH:mm:ss}. */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heureFermeture;
}
