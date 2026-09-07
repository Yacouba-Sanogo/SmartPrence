package com.smartpresence.service.impl;

import com.smartpresence.constants.DeviceStatut;
import com.smartpresence.dto.request.DeviceRequest;
import com.smartpresence.dto.response.DeviceResponse;
import com.smartpresence.entity.Device;
import com.smartpresence.entity.Salle;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.DeviceMapper;
import com.smartpresence.repository.DeviceRepository;
import com.smartpresence.repository.PresencePersonnelRepository;
import com.smartpresence.repository.PresenceRepository;
import com.smartpresence.repository.SalleRepository;
import com.smartpresence.service.DeviceService;
import com.smartpresence.utils.HachageCleApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final SalleRepository salleRepository;
    private final DeviceMapper deviceMapper;
    private final PresenceRepository presenceRepository;
    private final PresencePersonnelRepository presencePersonnelRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> findAll() {
        return deviceMapper.toResponseList(deviceRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceResponse findById(UUID id) {
        return deviceMapper.toResponse(getDevice(id));
    }

    @Override
    @Transactional
    public DeviceResponse create(DeviceRequest request) {
        String empreinte = HachageCleApi.empreinte(request.getApiKey());
        if (deviceRepository.existsByApiKeyHash(empreinte)) {
            throw new BusinessException(
                    "Un appareil existe déjà avec cette clé d'API", HttpStatus.CONFLICT);
        }
        if (deviceRepository.existsByAdresseMac(request.getAdresseMac())) {
            throw new BusinessException(
                    "Un appareil existe déjà avec l'adresse MAC : " + request.getAdresseMac(),
                    HttpStatus.CONFLICT);
        }
        Device device = deviceMapper.toEntity(request);
        device.setApiKeyHash(empreinte);
        device.setStatut(DeviceStatut.ACTIF);
        if (request.getSalleId() != null) {
            device.setSalle(getSalle(request.getSalleId()));
        }
        return deviceMapper.toResponse(deviceRepository.save(device));
    }

    @Override
    @Transactional
    public DeviceResponse update(UUID id, DeviceRequest request) {
        Device device = getDevice(id);
        // L'empreinte étant déterministe, cette comparaison a enfin un sens : avec BCrypt,
        // deux hachages de la même clé différaient toujours et le doublon passait.
        String nouvelleEmpreinte = HachageCleApi.empreinte(request.getApiKey());
        if (!nouvelleEmpreinte.equals(device.getApiKeyHash())
                && deviceRepository.existsByApiKeyHash(nouvelleEmpreinte)) {
            throw new BusinessException(
                    "Un appareil existe déjà avec cette clé d'API", HttpStatus.CONFLICT);
        }
        if (!device.getAdresseMac().equals(request.getAdresseMac())
                && deviceRepository.existsByAdresseMac(request.getAdresseMac())) {
            throw new BusinessException(
                    "Un appareil existe déjà avec l'adresse MAC : " + request.getAdresseMac(),
                    HttpStatus.CONFLICT);
        }
        deviceMapper.updateEntityFromRequest(request, device);
        device.setApiKeyHash(nouvelleEmpreinte);
        if (request.getSalleId() != null) {
            device.setSalle(getSalle(request.getSalleId()));
        } else {
            device.setSalle(null);
        }
        return deviceMapper.toResponse(deviceRepository.save(device));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Device device = getDevice(id);
        if (presenceRepository.countByDeviceId(id) > 0
                || presencePersonnelRepository.countByDeviceId(id) > 0) {
            throw new BusinessException(
                    "Impossible de supprimer un appareil rattaché à des présences ou à des pointages",
                    HttpStatus.CONFLICT);
        }
        deviceRepository.delete(device);
    }

    @Override
    @Transactional
    public void setStatut(UUID id, DeviceStatut statut) {
        Device device = getDevice(id);
        device.setStatut(statut);
        deviceRepository.save(device);
    }

    private Device getDevice(UUID id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", id));
    }

    private Salle getSalle(Long id) {
        return salleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle", "id", id));
    }
}
