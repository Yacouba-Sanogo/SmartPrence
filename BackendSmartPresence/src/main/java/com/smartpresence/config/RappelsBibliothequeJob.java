package com.smartpresence.config;

import com.smartpresence.service.BibliothequeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Rappels quotidiens de la bibliothèque : veille de retour, puis retard chaque semaine.
 *
 * <p>Tous les jours à 8 h par défaut ({@code app.bibliotheque.rappels-cron}) ;
 * {@code app.bibliotheque.rappels=false} désactive la tâche.</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.bibliotheque.rappels", havingValue = "true", matchIfMissing = true)
public class RappelsBibliothequeJob {

    private final BibliothequeService bibliotheque;

    @Scheduled(cron = "${app.bibliotheque.rappels-cron:0 0 8 * * *}")
    public void rappeler() {
        bibliotheque.envoyerRappels();
    }
}
