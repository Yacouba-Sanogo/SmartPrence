package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Un numéro CENOU du référentiel, tel que l'administration le consulte.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NumeroCenouResponse {

    private Long id;
    private String numero;
    private String nom;
    private String prenom;
    private Long classeId;
    private String classeCode;
    private boolean utilise;
    private Instant dateUtilisation;
    private Instant createdAt;
    private Instant updatedAt;
}
