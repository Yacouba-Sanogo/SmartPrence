package com.smartpresence.service.impl;

import com.smartpresence.dto.request.ParametreEtablissementRequest;
import com.smartpresence.dto.response.ParametreEtablissementResponse;
import com.smartpresence.entity.ParametreEtablissement;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.ParametreEtablissementMapper;
import com.smartpresence.repository.ParametreEtablissementRepository;
import com.smartpresence.service.ParametreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParametreServiceImpl implements ParametreService {

    private final ParametreEtablissementRepository parametreEtablissementRepository;
    private final ParametreEtablissementMapper parametreEtablissementMapper;

    @Override
    @Transactional(readOnly = true)
    public ParametreEtablissementResponse get() {
        ParametreEtablissement entity = parametreEtablissementRepository.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Paramètres établissement", "id", 1L));
        return parametreEtablissementMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public ParametreEtablissementResponse update(ParametreEtablissementRequest request) {
        ParametreEtablissement entity = parametreEtablissementRepository.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Paramètres établissement", "id", 1L));

        if (request.getNom() != null) entity.setNom(request.getNom());
        if (request.getSigle() != null) entity.setSigle(request.getSigle());
        if (request.getEmail() != null) entity.setEmail(request.getEmail());
        if (request.getTelephone() != null) entity.setTelephone(request.getTelephone());
        if (request.getFuseauHoraire() != null) entity.setFuseauHoraire(request.getFuseauHoraire());
        if (request.getSeuilRetardMinutes() != null) entity.setSeuilRetardMinutes(request.getSeuilRetardMinutes());
        if (request.getHeureOuverture() != null) entity.setHeureOuverture(request.getHeureOuverture());
        if (request.getHeureFermeture() != null) entity.setHeureFermeture(request.getHeureFermeture());

        if (!entity.getHeureFermeture().isAfter(entity.getHeureOuverture())) {
            throw new BusinessException("L'heure de fermeture doit être postérieure à l'heure d'ouverture");
        }

        return parametreEtablissementMapper.toResponse(parametreEtablissementRepository.save(entity));
    }
}
