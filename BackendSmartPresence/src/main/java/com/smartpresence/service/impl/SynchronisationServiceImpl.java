package com.smartpresence.service.impl;

import com.smartpresence.dto.response.HistoriqueSynchronisationResponse;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.HistoriqueSynchronisationMapper;
import com.smartpresence.repository.DeviceRepository;
import com.smartpresence.repository.HistoriqueSynchronisationRepository;
import com.smartpresence.service.SynchronisationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SynchronisationServiceImpl implements SynchronisationService {

    /** Plafond de sécurité : le journal grossit continuellement avec le parc déployé. */
    private static final int LIMITE_MAX = 200;
    private static final int LIMITE_DEFAUT = 50;

    private final HistoriqueSynchronisationRepository historiqueRepository;
    private final DeviceRepository deviceRepository;
    private final HistoriqueSynchronisationMapper historiqueMapper;

    @Override
    @Transactional(readOnly = true)
    public List<HistoriqueSynchronisationResponse> parAppareil(UUID deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("Appareil", "id", deviceId);
        }
        return historiqueMapper.toResponseList(
                historiqueRepository.findByDeviceIdOrderByDateHeureDesc(deviceId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoriqueSynchronisationResponse> recentes(int limite) {
        int taille = limite <= 0 ? LIMITE_DEFAUT : Math.min(limite, LIMITE_MAX);
        return historiqueMapper.toResponseList(
                historiqueRepository.findRecentes(PageRequest.of(0, taille)));
    }
}
