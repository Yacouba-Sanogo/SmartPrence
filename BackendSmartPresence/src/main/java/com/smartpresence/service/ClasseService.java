package com.smartpresence.service;

import com.smartpresence.dto.request.ClasseRequest;
import com.smartpresence.dto.response.ClasseDetailResponse;
import com.smartpresence.dto.response.ClasseResponse;

import java.util.List;
import java.util.UUID;

public interface ClasseService {

    List<ClasseResponse> findAll();

    ClasseDetailResponse findById(Long id);

    List<ClasseResponse> findByPromotionId(Long promotionId);

    ClasseResponse create(ClasseRequest request);

    ClasseResponse update(Long id, ClasseRequest request);

    /**
     * Désigne le responsable de la classe parmi ses étudiants ; {@code etudiantId} nul retire la
     * désignation. Seul l'administrateur le fait.
     */
    ClasseResponse designerResponsable(Long classeId, UUID etudiantId);

    void delete(Long id);
}
