package com.smartpresence.dto.response;

import com.smartpresence.constants.TypeNotification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Données d'une notification exposées via l'API.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private UUID destinataireId;
    private TypeNotification type;
    private String titre;
    private String message;
    private boolean lue;
    private Instant lueLe;
    private Instant createdAt;
}
