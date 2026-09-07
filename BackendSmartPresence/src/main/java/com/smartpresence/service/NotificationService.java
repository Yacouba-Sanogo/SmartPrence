package com.smartpresence.service;

import com.smartpresence.constants.TypeNotification;
import com.smartpresence.dto.response.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    List<NotificationResponse> findByDestinataire(UUID destinataireId);

    long countNonLues(UUID destinataireId);

    NotificationResponse createNotification(UUID destinataireId, TypeNotification type, String titre, String message);

    void marquerCommeLue(UUID id);
}
