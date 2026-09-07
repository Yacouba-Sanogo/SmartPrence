package com.smartpresence.service;

import com.smartpresence.dto.request.EtudiantRequest;
import com.smartpresence.dto.response.CompteEtudiantResponse;
import com.smartpresence.dto.response.EtudiantResponse;
import java.util.List;
import java.util.UUID;

public interface EtudiantService {

    List<EtudiantResponse> findAll(Long classeId);

    EtudiantResponse findById(UUID id);

    EtudiantResponse create(EtudiantRequest request);

    EtudiantResponse update(UUID id, EtudiantRequest request);

    void setActive(UUID id, boolean actif);

    /**
     * Associe à l'étudiant la référence logique de son empreinte.
     *
     * @param id          étudiant concerné
     * @param biometricId référence produite lors de la capture sur le lecteur
     */
    EtudiantResponse enroler(UUID id, String biometricId);

    /** Révoque l'enrôlement biométrique, en conservant les présences déjà relevées. */
    EtudiantResponse revoquerEnrolement(UUID id);

    /**
     * Ouvre un accès mobile à l'étudiant : crée son compte, lui attribue le rôle
     * {@code ETUDIANT} et le rattache à sa fiche.
     *
     * <p>Le mot de passe est tiré au hasard et retourné <b>une seule fois</b> : il est
     * haché avant enregistrement et le serveur ne peut plus le restituer.</p>
     *
     * @param id étudiant concerné
     * @return les identifiants à remettre à l'étudiant
     */
    CompteEtudiantResponse ouvrirCompte(UUID id);

    /**
     * Ferme l'accès mobile d'un étudiant et supprime son compte.
     *
     * <p>Les présences déjà relevées sont conservées : elles appartiennent au dossier
     * de scolarité, pas au compte de consultation.</p>
     *
     * @param id étudiant concerné
     */
    EtudiantResponse fermerCompte(UUID id);
}
