package com.smartpresence.dto.response;

import com.smartpresence.constants.SourcePresence;
import com.smartpresence.constants.StatutPresence;
import com.smartpresence.constants.StatutSeance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Feuille de présence d'une séance, telle que la consulte l'enseignant.
 *
 * <p>Elle est <b>constatée, non saisie</b> : chaque ligne reflète ce que le lecteur a
 * relevé. L'enseignant ne marque personne — il vérifie, et signale une anomalie le cas
 * échéant. C'est ce qui distingue ce système d'un appel manuel informatisé.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeuilleSeanceResponse {

    private UUID seanceId;
    private String matiereLibelle;
    private Long classeId;
    private String classeCode;
    private String classeLibelle;
    private String salleLibelle;
    private Instant debut;
    private Instant fin;
    private StatutSeance statut;

    /** Nombre d'étudiants inscrits dans la classe. */
    private int effectif;

    private int presents;
    private int retards;
    private int absents;

    /**
     * Étudiants non enrôlés parmi les absents.
     *
     * <p>Compté à part parce que leur absence de relevé <b>ne dit rien</b> de leur
     * assiduité : faute d'empreinte associée, le lecteur ne pouvait pas les identifier.
     * Les confondre avec de vrais absents fausserait le jugement de l'enseignant.</p>
     */
    private int absentsNonEnroles;

    private List<LigneFeuilleResponse> lignes;

    /** Situation d'un étudiant pour la séance. */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LigneFeuilleResponse {
        private UUID etudiantId;
        private String matricule;
        private String nom;
        private String prenom;

        /** {@code false} : l'étudiant ne peut pas être relevé par un lecteur. */
        private boolean enrole;

        private StatutPresence statut;

        /** Heure du relevé, {@code null} si l'étudiant n'a pas été identifié. */
        private LocalTime heure;

        /** Origine du relevé, {@code null} en l'absence de relevé. */
        private SourcePresence source;
    }
}
