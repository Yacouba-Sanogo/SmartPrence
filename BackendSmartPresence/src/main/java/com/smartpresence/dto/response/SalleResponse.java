package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Données d'une salle exposées via l'API REST.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalleResponse {

    private Long id;
    private String code;
    private String nom;
    private String batiment;
    private Integer capacite;
    private UUID deviceId;
    private String deviceNom;
    private Instant createdAt;
    private Instant updatedAt;
}
