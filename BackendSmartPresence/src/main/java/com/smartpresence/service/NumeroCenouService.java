package com.smartpresence.service;

import com.smartpresence.dto.request.NumeroCenouRequest;
import com.smartpresence.dto.response.ImportCenouResponse;
import com.smartpresence.dto.response.NumeroCenouResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Référentiel des numéros CENOU autorisés à s'inscrire.
 *
 * <p>C'est la liste blanche de l'inscription libre : sans numéro connu ici, aucune
 * inscription n'aboutit.</p>
 */
public interface NumeroCenouService {

    /**
     * Liste le référentiel.
     *
     * @param utilise {@code null} pour tout voir, sinon filtre sur les numéros déjà
     *                consommés ou encore disponibles
     */
    List<NumeroCenouResponse> findAll(Boolean utilise);

    /** Ajoute un numéro à la main. */
    NumeroCenouResponse create(NumeroCenouRequest request);

    /**
     * Retire un numéro du référentiel.
     *
     * @throws com.smartpresence.exception.BusinessException si le numéro a déjà servi :
     *         le supprimer rouvrirait une inscription au nom d'un étudiant existant
     */
    void delete(Long id);

    /**
     * Alimente le référentiel depuis un tableur {@code .xlsx} ou un fichier
     * {@code .csv}.
     *
     * <p>Colonnes attendues, dans cet ordre : numéro, nom, prénom. Seule la première
     * est obligatoire. Une éventuelle ligne d'en-tête est reconnue et ignorée.</p>
     */
    ImportCenouResponse importer(MultipartFile fichier, Long classeId);
}
