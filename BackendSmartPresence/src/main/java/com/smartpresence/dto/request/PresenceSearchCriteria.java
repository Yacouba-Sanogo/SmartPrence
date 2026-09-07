package com.smartpresence.dto.request;

import com.smartpresence.constants.SourcePresence;
import com.smartpresence.constants.StatutPresence;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Critères de recherche filtrée pour les présences.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceSearchCriteria {

    private UUID etudiantId;
    private Long classeId;
    private Long promotionId;
    private UUID deviceId;
    private Long salleId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateDebut;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFin;

    private StatutPresence statut;
    private SourcePresence source;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
