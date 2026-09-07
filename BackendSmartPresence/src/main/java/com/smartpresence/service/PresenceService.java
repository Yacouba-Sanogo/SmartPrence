package com.smartpresence.service;

import com.smartpresence.dto.request.PresenceESP32Request;
import com.smartpresence.dto.request.PresenceManuelRequest;
import com.smartpresence.dto.request.PresenceSearchCriteria;
import com.smartpresence.dto.response.PagedResponse;
import com.smartpresence.dto.response.PresenceResponse;
import com.smartpresence.dto.response.SyncSummaryResponse;

import java.util.UUID;

/** Business operations for attendance capture, consultation and ESP32 synchronisation. */
public interface PresenceService {

    PagedResponse<PresenceResponse> search(PresenceSearchCriteria criteria);

    PresenceResponse createManual(PresenceManuelRequest request);

    SyncSummaryResponse synchronize(UUID authenticatedDeviceId, PresenceESP32Request request);
}
