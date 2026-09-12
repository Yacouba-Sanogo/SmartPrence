package com.smartpresence.service;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.dto.request.UniteEnseignementRequest;
import com.smartpresence.dto.response.UniteEnseignementResponse;

import java.util.List;

/**
 * Maquette pédagogique : les unités d'enseignement et leurs crédits.
 *
 * @since 0.0.1
 */
public interface UniteEnseignementService {

    /** Maquette, filtrable par promotion et par semestre. */
    List<UniteEnseignementResponse> findAll(Long promotionId, PeriodeScolaire semestre);

    UniteEnseignementResponse findById(Long id);

    UniteEnseignementResponse create(UniteEnseignementRequest request);

    UniteEnseignementResponse update(Long id, UniteEnseignementRequest request);

    /**
     * Retire une UE de la maquette.
     *
     * @throws com.smartpresence.exception.BusinessException si des ECUE lui sont
     *         encore rattachés — les détacher est un geste délibéré, pas un effet de bord
     */
    void delete(Long id);
}
