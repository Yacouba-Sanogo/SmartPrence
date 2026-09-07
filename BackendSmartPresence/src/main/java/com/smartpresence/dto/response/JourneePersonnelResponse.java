package com.smartpresence.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartpresence.constants.StatutPresence;
import com.smartpresence.constants.TypePersonnel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Synthèse de la journée de travail d'un agent.
 *
 * <p>Vue agrégée exploitable directement par l'interface d'administration : elle condense
 * la suite brute des pointages en une ligne lisible — heure d'arrivée, heure de départ,
 * retard éventuel et temps de présence effectif.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JourneePersonnelResponse {

    private UUID personnelId;
    private String matricule;
    private String nom;
    private String prenom;
    private TypePersonnel type;
    private String service;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /** Heure du premier pointage d'entrée. {@code null} si l'agent n'a pas pointé. */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heureEntree;

    /** Heure du dernier pointage de sortie. {@code null} si l'agent est encore présent. */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heureSortie;

    /**
     * Qualification de la journée : {@code PRESENT}, {@code RETARD} ou {@code ABSENT}
     * si aucun pointage n'a été relevé.
     */
    private StatutPresence statut;

    /** Minutes de retard sur l'heure d'ouverture, {@code 0} si l'agent est à l'heure. */
    private long minutesRetard;

    /**
     * Temps de présence effectif en minutes, calculé entre l'entrée et la sortie.
     * <p>{@code null} tant que l'agent n'a pas pointé sa sortie.</p>
     */
    private Long minutesTravaillees;

    /**
     * Temps écoulé depuis l'arrivée, en minutes, pour un agent <b>encore sur site</b>.
     *
     * <p>Complète {@link #minutesTravaillees}, qui ne peut être calculé qu'une fois la
     * sortie pointée : sans ce champ, l'interface n'aurait rien à afficher pour les agents
     * présents, qui sont précisément ceux que le service RH observe en cours de journée.</p>
     *
     * <p>{@code null} dès que la sortie est pointée, et pour toute date passée — un temps
     * « écoulé » n'aurait alors aucun sens.</p>
     */
    private Long minutesEcoulees;

    /** Nombre total de pointages relevés dans la journée (entrées et sorties). */
    private int nombrePointages;

    /** {@code true} si l'agent est entré sans avoir encore pointé sa sortie. */
    private boolean present;
}
