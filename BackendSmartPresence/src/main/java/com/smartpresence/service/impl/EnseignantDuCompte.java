package com.smartpresence.service.impl;

import com.smartpresence.constants.TypePersonnel;
import com.smartpresence.entity.Personnel;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.PersonnelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Résout l'enseignant derrière un compte et vérifie son rattachement aux classes.
 *
 * <p>Le sujet vient toujours du jeton, jamais de la requête : c'est ce qui rend le
 * cloisonnement structurel.</p>
 */
@Component
@RequiredArgsConstructor
class EnseignantDuCompte {

    private final PersonnelRepository personnelRepository;
    private final ClasseRepository classeRepository;

    Personnel resoudre(UUID utilisateurId) {
        Personnel personnel = personnelRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new BusinessException(
                        "Ce compte n'est rattaché à aucune fiche d'agent", HttpStatus.FORBIDDEN));
        if (personnel.getType() != TypePersonnel.ENSEIGNANT) {
            throw new BusinessException(
                    "Ce compte n'est pas rattaché à un enseignant", HttpStatus.FORBIDDEN);
        }
        return personnel;
    }

    void exigerClasse(Personnel enseignant, Long classeId) {
        boolean rattache = classeRepository.findParEnseignant(enseignant.getId()).stream()
                .anyMatch(classe -> classe.getId().equals(classeId));
        if (!rattache) {
            throw new BusinessException("Vous n'intervenez pas dans cette classe", HttpStatus.FORBIDDEN);
        }
    }
}
