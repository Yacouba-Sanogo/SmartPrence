package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Données d'une promotion exposées via l'API REST.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponse {

    private Long id;
    private String code;
    private String libelle;
    private Integer anneeAcademique;
    private Instant createdAt;
    private Instant updatedAt;
}
