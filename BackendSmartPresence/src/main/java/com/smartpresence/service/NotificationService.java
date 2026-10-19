package com.smartpresence.service;

import com.smartpresence.constants.TypeNotification;
import com.smartpresence.dto.response.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    List<NotificationResponse> findByDestinataire(UUID destinataireId);

    long countNonLues(UUID destinataireId);

    NotificationResponse createNotification(UUID destinataireId, TypeNotification type, String titre, String message);

    /** Marque lue une notification du destinataire ; celle d'un autre est refusée. */
    void marquerCommeLue(UUID destinataireId, UUID id);

    /** Marque lues toutes les notifications du destinataire ; renvoie leur nombre. */
    int toutMarquerCommeLu(UUID destinataireId);

    /**
     * Diffuse une annonce aux enseignants : un seul s'il est nommé, sinon ceux d'une classe,
     * sinon tous les enseignants actifs. Renvoie le nombre de comptes notifiés — un
     * enseignant sans compte mobile ne reçoit rien et n'est pas compté.
     */
    int diffuserAuxEnseignants(TypeNotification type, String titre, String message,
                               Long classeId, UUID personnelId);

    /** Notifie un enseignant, sans rien faire s'il n'a pas de compte. */
    void notifierEnseignant(com.smartpresence.entity.Personnel enseignant, TypeNotification type,
                            String titre, String message);
}
