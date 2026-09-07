package com.smartpresence.service.impl;

import com.smartpresence.constants.TypeNotification;
import com.smartpresence.dto.response.NotificationResponse;
import com.smartpresence.entity.Notification;
import com.smartpresence.entity.Utilisateur;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.NotificationMapper;
import com.smartpresence.repository.NotificationRepository;
import com.smartpresence.repository.UtilisateurRepository;
import com.smartpresence.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> findByDestinataire(UUID destinataireId) {
        return notificationMapper.toResponseList(
                notificationRepository.findByDestinataireIdOrderByCreatedAtDesc(destinataireId));
    }

    @Override
    @Transactional(readOnly = true)
    public long countNonLues(UUID destinataireId) {
        return notificationRepository.countByDestinataireIdAndLueFalse(destinataireId);
    }

    @Override
    @Transactional
    public NotificationResponse createNotification(UUID destinataireId, TypeNotification type, String titre, String message) {
        Utilisateur destinataire = utilisateurRepository.findById(destinataireId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", destinataireId));

        Notification notification = new Notification();
        notification.setDestinataire(destinataire);
        notification.setType(type);
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setLue(false);

        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void marquerCommeLue(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        notification.setLue(true);
        notification.setLueLe(Instant.now());
        notificationRepository.save(notification);
    }
}
