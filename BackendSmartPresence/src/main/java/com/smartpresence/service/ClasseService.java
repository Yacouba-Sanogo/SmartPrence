package com.smartpresence.service;

import com.smartpresence.dto.request.ClasseRequest;
import com.smartpresence.dto.response.ClasseDetailResponse;
import com.smartpresence.dto.response.ClasseResponse;

import java.util.List;

public interface ClasseService {

    List<ClasseResponse> findAll();

    ClasseDetailResponse findById(Long id);

    List<ClasseResponse> findByPromotionId(Long promotionId);

    ClasseResponse create(ClasseRequest request);

    ClasseResponse update(Long id, ClasseRequest request);

    void delete(Long id);
}
