package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Données détaillées d'une classe avec liste de ses enseignants et étudiants.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClasseDetailResponse {

    private Long id;
    private String code;
    private String libelle;
    private PromotionResponse promotion;
    /** Enseignants rattachés — des agents de catégorie ENSEIGNANT. */
    private Set<PersonnelResponse> enseignants;
    private List<EtudiantResponse> etudiants;
    private Instant createdAt;
    private Instant updatedAt;
}
