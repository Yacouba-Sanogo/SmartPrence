package com.smartpresence.config;

import com.smartpresence.dto.response.PassageResponse;
import com.smartpresence.service.ParcoursService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Passage automatique en année supérieure, une fois par an.
 *
 * <p>Par défaut le 1er septembre à 3 h (réglable par {@code app.passage.cron}). Seuls
 * les étudiants dont tous les semestres sont validés montent : un étudiant dont les
 * notes manquent n'est jamais déplacé. {@code app.passage.automatique=false}
 * désactive la tâche ; le passage reste alors déclenchable à la main.</p>
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.passage.automatique", havingValue = "true", matchIfMissing = true)
public class PassageAutomatiqueJob {

    private final ParcoursService parcoursService;

    @Scheduled(cron = "${app.passage.cron:0 0 3 1 9 *}")
    public void passer() {
        PassageResponse r = parcoursService.passage(null, true);
        log.info("Passage annuel automatique : {} promu(s), {} redoublant(s), {} en attente, "
                        + "{} sans classe supérieure, {} option(s) à choisir",
                r.promus(), r.redoublants(), r.enAttente(), r.termines(), r.optionsManquantes());
    }
}
