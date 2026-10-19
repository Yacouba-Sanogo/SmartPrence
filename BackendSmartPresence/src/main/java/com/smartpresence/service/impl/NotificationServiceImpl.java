package com.smartpresence.service.impl;

import com.smartpresence.constants.TypeNotification;
import com.smartpresence.dto.response.NotificationResponse;
import com.smartpresence.constants.TypePersonnel;
import com.smartpresence.entity.Notification;
import com.smartpresence.entity.Personnel;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.PersonnelRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PersonnelRepository personnelRepository;
    private final ClasseRepository classeRepository;
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
    public void marquerCommeLue(UUID destinataireId, UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        // Une notification est personnelle : sans ce contrôle, n'importe quel compte
        // connecté pouvait marquer lue celle d'un autre en devinant son identifiant.
        if (!notification.getDestinataire().getId().equals(destinataireId)) {
            throw new BusinessException("Cette notification ne vous est pas destinée", HttpStatus.FORBIDDEN);
        }
        if (!notification.isLue()) {
            notification.setLue(true);
            notification.setLueLe(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public int toutMarquerCommeLu(UUID destinataireId) {
        List<Notification> nonLues = notificationRepository.findByDestinataireIdAndLueFalse(destinataireId);
        Instant maintenant = Instant.now();
        nonLues.forEach(n -> {
            n.setLue(true);
            n.setLueLe(maintenant);
        });
        notificationRepository.saveAll(nonLues);
        return nonLues.size();
    }

    @Override
    @Transactional
    public int diffuserAuxEnseignants(TypeNotification type, String titre, String message,
                                      Long classeId, UUID personnelId) {
        List<Personnel> cibles;
        if (personnelId != null) {
            cibles = List.of(personnelRepository.findById(personnelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Personnel", "id", personnelId)));
        } else if (classeId != null) {
            cibles = List.copyOf(classeRepository.findById(classeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Classe", "id", classeId))
                    .getEnseignants());
        } else {
            cibles = personnelRepository.findByTypeOrderByNomAscPrenomAsc(TypePersonnel.ENSEIGNANT)
                    .stream().filter(Personnel::isActif).toList();
        }

        int notifies = 0;
        for (Personnel enseignant : cibles) {
            if (enseignant.getType() == TypePersonnel.ENSEIGNANT && enseignant.getUtilisateur() != null) {
                createNotification(enseignant.getUtilisateur().getId(), type, titre, message);
                notifies++;
            }
        }
        log.info("Annonce « {} » diffusée à {} enseignant(s)", titre, notifies);
        return notifies;
    }

    @Override
    @Transactional
    public void notifierEnseignant(Personnel enseignant, TypeNotification type, String titre, String message) {
        if (enseignant != null && enseignant.getUtilisateur() != null) {
            createNotification(enseignant.getUtilisateur().getId(), type, titre, message);
        }
    }
}
