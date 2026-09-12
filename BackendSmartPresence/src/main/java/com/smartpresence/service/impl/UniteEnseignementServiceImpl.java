package com.smartpresence.service.impl;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.dto.request.UniteEnseignementRequest;
import com.smartpresence.dto.response.UniteEnseignementResponse;
import com.smartpresence.entity.Matiere;
import com.smartpresence.entity.Promotion;
import com.smartpresence.entity.UniteEnseignement;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.repository.MatiereRepository;
import com.smartpresence.repository.PromotionRepository;
import com.smartpresence.repository.UniteEnseignementRepository;
import com.smartpresence.service.UniteEnseignementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Maquette pédagogique : les unités d'enseignement et leurs crédits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UniteEnseignementServiceImpl implements UniteEnseignementService {

    private final UniteEnseignementRepository uniteRepository;
    private final PromotionRepository promotionRepository;
    private final MatiereRepository matiereRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UniteEnseignementResponse> findAll(Long promotionId, PeriodeScolaire semestre) {
        List<UniteEnseignement> unites;
        if (promotionId != null && semestre != null) {
            unites = uniteRepository.findByPromotionIdAndSemestreOrderByCodeAsc(promotionId, semestre);
        } else if (promotionId != null) {
            unites = uniteRepository.findByPromotionIdOrderBySemestreAscCodeAsc(promotionId);
        } else {
            unites = uniteRepository.findAll().stream()
                    .filter(unite -> semestre == null || unite.getSemestre() == semestre)
                    .sorted((a, b) -> {
                        int parSemestre = Integer.compare(a.getSemestre().rang(), b.getSemestre().rang());
                        return parSemestre != 0 ? parSemestre : a.getCode().compareToIgnoreCase(b.getCode());
                    })
                    .toList();
        }
        return unites.stream().map(this::versReponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UniteEnseignementResponse findById(Long id) {
        return versReponse(unite(id));
    }

    @Override
    @Transactional
    public UniteEnseignementResponse create(UniteEnseignementRequest request) {
        String code = normaliser(request.getCode());
        if (uniteRepository.existsByCode(code)) {
            throw new BusinessException("Ce code d'UE existe déjà", HttpStatus.CONFLICT);
        }

        UniteEnseignement unite = new UniteEnseignement();
        appliquer(unite, request, code);
        UniteEnseignement enregistree = uniteRepository.save(unite);
        log.info("UE créée — code={}, {} crédits, {}",
                enregistree.getCode(), enregistree.getCredits(), enregistree.getSemestre());
        return versReponse(enregistree);
    }

    @Override
    @Transactional
    public UniteEnseignementResponse update(Long id, UniteEnseignementRequest request) {
        UniteEnseignement unite = unite(id);
        String code = normaliser(request.getCode());
        if (!unite.getCode().equals(code) && uniteRepository.existsByCode(code)) {
            throw new BusinessException("Ce code d'UE existe déjà", HttpStatus.CONFLICT);
        }
        appliquer(unite, request, code);
        return versReponse(uniteRepository.save(unite));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        UniteEnseignement unite = unite(id);

        // Supprimer l'UE détacherait ses ECUE en silence, et les notes déjà saisies
        // sortiraient du relevé sans que personne ne l'ait demandé.
        List<Matiere> ecues = matiereRepository.findByUniteEnseignementId(id);
        if (!ecues.isEmpty()) {
            throw new BusinessException(
                    "Cette UE regroupe encore " + ecues.size() + " matière(s). "
                            + "Détachez-les avant de la supprimer.", HttpStatus.CONFLICT);
        }

        uniteRepository.delete(unite);
        log.info("UE supprimée — code={}", unite.getCode());
    }

    // ------------------------------------------------------------------

    private void appliquer(UniteEnseignement unite, UniteEnseignementRequest request, String code) {
        Promotion promotion = promotionRepository.findById(request.getPromotionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Promotion", "id", request.getPromotionId()));

        unite.setCode(code);
        unite.setLibelle(request.getLibelle().trim());
        unite.setCredits(request.getCredits());
        unite.setSemestre(request.getSemestre());
        unite.setPromotion(promotion);
        if (request.getActive() != null) {
            unite.setActive(request.getActive());
        }
    }

    private UniteEnseignement unite(Long id) {
        return uniteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unité d'enseignement", "id", id));
    }

    private static String normaliser(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    private UniteEnseignementResponse versReponse(UniteEnseignement unite) {
        List<Matiere> ecues = matiereRepository.findByUniteEnseignementId(unite.getId());
        Promotion promotion = unite.getPromotion();

        return UniteEnseignementResponse.builder()
                .id(unite.getId())
                .code(unite.getCode())
                .libelle(unite.getLibelle())
                .credits(unite.getCredits())
                .semestre(unite.getSemestre())
                .semestreLibelle(unite.getSemestre().libelleCourt())
                .promotionId(promotion == null ? null : promotion.getId())
                .promotionLibelle(promotion == null ? null : promotion.getLibelle())
                .active(unite.isActive())
                .ecues(ecues.stream()
                        .map(ecue -> UniteEnseignementResponse.EcueBrefResponse.builder()
                                .matiereId(ecue.getId())
                                .code(ecue.getCode())
                                .libelle(ecue.getLibelle())
                                .credits(ecue.getCredits())
                                .active(ecue.isActive())
                                .build())
                        .toList())
                .creditsEcues(ecues.stream()
                        .mapToInt(ecue -> ecue.getCredits() == null ? 0 : ecue.getCredits())
                        .sum())
                .build();
    }
}
