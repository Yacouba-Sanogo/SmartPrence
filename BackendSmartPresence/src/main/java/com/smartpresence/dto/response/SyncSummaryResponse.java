package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Résumé retourné à l'ESP32 à l'issue d'une synchronisation REST.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncSummaryResponse {

    private UUID syncId;
    private UUID deviceId;
    private int totalTraites;
    private int totalInseres;
    private int totalIgnoresDoublons;
    private Instant synchronizedAt;
}
