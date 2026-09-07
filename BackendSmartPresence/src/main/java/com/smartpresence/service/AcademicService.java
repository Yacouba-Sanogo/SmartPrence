package com.smartpresence.service;

import com.smartpresence.dto.request.MatiereRequest;
import com.smartpresence.dto.request.SeanceRequest;
import com.smartpresence.dto.response.MatiereResponse;
import com.smartpresence.dto.response.SeanceResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Référentiel des matières et planification des séances.
 *
 * @since 0.0.1
 */
public interface AcademicService {

    List<MatiereResponse> matieres();

    MatiereResponse createMatiere(MatiereRequest request);

    MatiereResponse updateMatiere(Long id, MatiereRequest request);

    /**
     * Supprime une matière.
     *
     * @throws com.smartpresence.exception.BusinessException si des séances s'y rattachent —
     *         les supprimer en cascade effacerait des relevés de présence
     */
    void deleteMatiere(Long id);

    /**
     * Séances d'une période, filtrables.
     *
     * @param debut        premier jour observé, inclus
     * @param fin          dernier jour observé, inclus
     * @param classeId     filtre facultatif
     * @param enseignantId filtre facultatif
     */
    List<SeanceResponse> seances(LocalDate debut, LocalDate fin, Long classeId, UUID enseignantId);

    SeanceResponse createSeance(SeanceRequest request);

    /**
     * Modifie une séance planifiée.
     *
     * @throws com.smartpresence.exception.BusinessException si la séance a déjà commencé —
     *         déplacer un cours dont les présences sont relevées invaliderait ces relevés
     */
    SeanceResponse updateSeance(UUID id, SeanceRequest request);

    /**
     * Supprime une séance.
     *
     * @throws com.smartpresence.exception.BusinessException si des présences y sont
     *         rattachées ; il faut alors l'annuler plutôt que l'effacer
     */
    void deleteSeance(UUID id);

    void changeStatutSeance(UUID id, String statut);
}
