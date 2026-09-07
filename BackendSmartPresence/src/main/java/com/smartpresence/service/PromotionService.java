package com.smartpresence.service;

import com.smartpresence.dto.request.PromotionRequest;
import com.smartpresence.dto.response.PromotionResponse;

import java.util.List;

public interface PromotionService {

    List<PromotionResponse> findAll();

    PromotionResponse findById(Long id);

    PromotionResponse create(PromotionRequest request);

    PromotionResponse update(Long id, PromotionRequest request);

    void delete(Long id);
}
