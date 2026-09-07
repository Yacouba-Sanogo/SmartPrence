package com.smartpresence.service;

import com.smartpresence.constants.DeviceStatut;
import com.smartpresence.dto.request.DeviceRequest;
import com.smartpresence.dto.response.DeviceResponse;

import java.util.List;
import java.util.UUID;

public interface DeviceService {

    List<DeviceResponse> findAll();

    DeviceResponse findById(UUID id);

    DeviceResponse create(DeviceRequest request);

    DeviceResponse update(UUID id, DeviceRequest request);

    void delete(UUID id);

    void setStatut(UUID id, DeviceStatut statut);
}
