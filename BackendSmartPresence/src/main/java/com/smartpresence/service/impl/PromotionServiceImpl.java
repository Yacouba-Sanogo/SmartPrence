package com.smartpresence.service.impl;

import com.smartpresence.dto.request.PromotionRequest;
import com.smartpresence.dto.response.PromotionResponse;
import com.smartpresence.entity.Promotion;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.PromotionMapper;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.PromotionRepository;
import com.smartpresence.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final ClasseRepository classeRepository;
    private final PromotionMapper promotionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> findAll() {
        return promotionMapper.toResponseList(promotionRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse findById(Long id) {
        return promotionMapper.toResponse(getPromotion(id));
    }

    @Override
    @Transactional
    public PromotionResponse create(PromotionRequest request) {
        if (promotionRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Une promotion existe déjà avec le code : " + request.getCode());
        }
        Promotion promotion = promotionMapper.toEntity(request);
        return promotionMapper.toResponse(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public PromotionResponse update(Long id, PromotionRequest request) {
        Promotion promotion = getPromotion(id);
        if (!promotion.getCode().equals(request.getCode()) && promotionRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Une promotion existe déjà avec le code : " + request.getCode());
        }
        promotionMapper.updateEntityFromRequest(request, promotion);
        return promotionMapper.toResponse(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Promotion promotion = getPromotion(id);
        if (!classeRepository.findByPromotionId(id).isEmpty()) {
            throw new BusinessException("Impossible de supprimer une promotion contenant des classes", HttpStatus.CONFLICT);
        }
        promotionRepository.delete(promotion);
    }

    private Promotion getPromotion(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", id));
    }
}
