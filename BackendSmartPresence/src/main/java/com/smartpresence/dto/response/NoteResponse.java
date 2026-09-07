package com.smartpresence.dto.response;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.constants.TypeEvaluation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Note exposée via l'API.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteResponse {

    private UUID id;

    private UUID etudiantId;
    private String etudiantNom;
    private String etudiantPrenom;
    private String etudiantMatricule;

    private Long matiereId;
    private String matiereCode;
    private String matiereLibelle;

    private UUID enseignantId;
    private String enseignantNom;

    private BigDecimal valeur;
    private int coefficient;
    private TypeEvaluation type;
    private PeriodeScolaire periode;
    private String libelle;
    private LocalDate dateEvaluation;
    private String appreciation;
}
