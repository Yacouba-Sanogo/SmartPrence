package com.smartpresence.service.impl;

import com.smartpresence.constants.DeviceStatut;
import com.smartpresence.constants.StatutPresence;
import com.smartpresence.dto.response.StatistiquesClasseResponse;
import com.smartpresence.dto.response.StatistiquesEtudiantResponse;
import com.smartpresence.dto.response.StatistiquesGlobalesResponse;
import com.smartpresence.entity.Classe;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.DeviceRepository;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.PresenceRepository;
import com.smartpresence.repository.PresenceSpecification;
import com.smartpresence.service.StatistiquesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatistiquesServiceImpl implements StatistiquesService {

    private final PresenceRepository presenceRepository;
    private final EtudiantRepository etudiantRepository;
    private final ClasseRepository classeRepository;
    private final DeviceRepository deviceRepository;

    @Override
    @Transactional(readOnly = true)
    public StatistiquesGlobalesResponse getGlobales() {
        long totalEtudiants = etudiantRepository.count();
        long totalClasses = classeRepository.count();
        long totalDevicesActifs = deviceRepository.countByStatut(DeviceStatut.ACTIF);
        long totalPresences = presenceRepository.count();
        long totalPresents = presenceRepository.countByStatut(StatutPresence.PRESENT);
        long totalRetards = presenceRepository.countByStatut(StatutPresence.RETARD);
        long totalAbsents = presenceRepository.countByStatut(StatutPresence.ABSENT);
        long totalJustifies = presenceRepository.countByStatut(StatutPresence.JUSTIFIE);
        double tauxAssiduite = totalPresences > 0
                ? ((double) (totalPresents + totalRetards) / totalPresences) * 100.0
                : 0.0;

        return StatistiquesGlobalesResponse.builder()
                .totalEtudiants(totalEtudiants)
                .totalClasses(totalClasses)
                .totalDevicesActifs(totalDevicesActifs)
                .totalPresencesEnregistrees(totalPresences)
                .totalPresents(totalPresents)
                .totalRetards(totalRetards)
                .totalAbsents(totalAbsents)
                .totalJustifies(totalJustifies)
                .tauxAssiduite(tauxAssiduite)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StatistiquesClasseResponse getByClasse(Long classeId) {
        Classe classe = classeRepository.findById(classeId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe", "id", classeId));

        long totalEtudiants = etudiantRepository.countByClasseId(classeId);
        long totalPresences = presenceRepository.countByEtudiantClasseId(classeId);
        long totalPresents = presenceRepository.countByEtudiantClasseIdAndStatut(classeId, StatutPresence.PRESENT);
        long totalRetards = presenceRepository.countByEtudiantClasseIdAndStatut(classeId, StatutPresence.RETARD);
        long totalAbsents = presenceRepository.countByEtudiantClasseIdAndStatut(classeId, StatutPresence.ABSENT);
        long totalJustifies = presenceRepository.countByEtudiantClasseIdAndStatut(classeId, StatutPresence.JUSTIFIE);
        double tauxAssiduite = totalPresences > 0
                ? ((double) (totalPresents + totalRetards) / totalPresences) * 100.0
                : 0.0;

        return StatistiquesClasseResponse.builder()
                .classeId(classeId)
                .classeCode(classe.getCode())
                .classeLibelle(classe.getLibelle())
                .totalEtudiants(totalEtudiants)
                .totalPresences(totalPresences)
                .totalPresents(totalPresents)
                .totalRetards(totalRetards)
                .totalAbsents(totalAbsents)
                .totalJustifies(totalJustifies)
                .tauxAssiduite(tauxAssiduite)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StatistiquesEtudiantResponse getByEtudiant(UUID etudiantId) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant", "id", etudiantId));

        long totalPresences = presenceRepository.countByEtudiantId(etudiantId);
        long totalPresents = presenceRepository.countByEtudiantIdAndStatut(etudiantId, StatutPresence.PRESENT);
        long totalRetards = presenceRepository.countByEtudiantIdAndStatut(etudiantId, StatutPresence.RETARD);
        long totalAbsents = presenceRepository.countByEtudiantIdAndStatut(etudiantId, StatutPresence.ABSENT);
        long totalJustifies = presenceRepository.countByEtudiantIdAndStatut(etudiantId, StatutPresence.JUSTIFIE);
        double tauxAssiduite = totalPresences > 0
                ? ((double) (totalPresents + totalRetards) / totalPresences) * 100.0
                : 0.0;

        return StatistiquesEtudiantResponse.builder()
                .etudiantId(etudiantId)
                .matricule(etudiant.getMatricule())
                .nom(etudiant.getNom())
                .prenom(etudiant.getPrenom())
                .classeCode(etudiant.getClasse().getCode())
                .totalPresences(totalPresences)
                .totalPresents(totalPresents)
                .totalRetards(totalRetards)
                .totalAbsents(totalAbsents)
                .totalJustifies(totalJustifies)
                .tauxAssiduite(tauxAssiduite)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getTendanceJournaliere(LocalDate debut, LocalDate fin) {
        Map<String, Long> tendance = new LinkedHashMap<>();
        LocalDate current = debut;
        while (!current.isAfter(fin)) {
            long count = presenceRepository.count(PresenceSpecification.withCriteria(
                    com.smartpresence.dto.request.PresenceSearchCriteria.builder()
                            .dateDebut(current)
                            .dateFin(current)
                            .build()));
            tendance.put(current.toString(), count);
            current = current.plusDays(1);
        }
        return tendance;
    }
}
