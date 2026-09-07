package com.smartpresence.service.impl;

import com.smartpresence.dto.request.SalleRequest;
import com.smartpresence.dto.response.SalleResponse;
import com.smartpresence.entity.Salle;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.SalleMapper;
import com.smartpresence.repository.DeviceRepository;
import com.smartpresence.repository.SalleRepository;
import com.smartpresence.service.SalleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalleServiceImpl implements SalleService {

    private final SalleRepository salleRepository;
    private final DeviceRepository deviceRepository;
    private final SalleMapper salleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SalleResponse> findAll() {
        return salleMapper.toResponseList(salleRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public SalleResponse findById(Long id) {
        return salleMapper.toResponse(getSalle(id));
    }

    @Override
    @Transactional
    public SalleResponse create(SalleRequest request) {
        if (salleRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Une salle existe déjà avec le code : " + request.getCode());
        }
        Salle salle = salleMapper.toEntity(request);
        return salleMapper.toResponse(salleRepository.save(salle));
    }

    @Override
    @Transactional
    public SalleResponse update(Long id, SalleRequest request) {
        Salle salle = getSalle(id);
        if (!salle.getCode().equals(request.getCode()) && salleRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Une salle existe déjà avec le code : " + request.getCode());
        }
        salleMapper.updateEntityFromRequest(request, salle);
        return salleMapper.toResponse(salleRepository.save(salle));
    }

    /**
     * Supprime une salle libre de tout équipement.
     *
     * <p>Un lecteur ESP32 référence sa salle par clé étrangère : supprimer une salle
     * équipée violerait la contrainte d'intégrité et remonterait en erreur technique
     * plutôt qu'en refus métier compréhensible. Le lecteur doit d'abord être déplacé
     * ou détaché.</p>
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Salle salle = getSalle(id);
        deviceRepository.findBySalleId(id).ifPresent(device -> {
            throw new BusinessException(
                    "La salle est équipée du lecteur « " + device.getNom()
                            + " » : détachez-le avant de la supprimer",
                    HttpStatus.CONFLICT);
        });
        salleRepository.delete(salle);
    }

    private Salle getSalle(Long id) {
        return salleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle", "id", id));
    }
}
