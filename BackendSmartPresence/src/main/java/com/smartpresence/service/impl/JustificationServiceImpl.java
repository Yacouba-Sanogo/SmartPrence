package com.smartpresence.service.impl;

import com.smartpresence.constants.StatutJustification;
import com.smartpresence.dto.response.JustificationAbsenceResponse;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.entity.JustificationAbsence;
import com.smartpresence.entity.Seance;
import com.smartpresence.entity.Utilisateur;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.JustificationAbsenceMapper;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.JustificationAbsenceRepository;
import com.smartpresence.repository.SeanceRepository;
import com.smartpresence.repository.UtilisateurRepository;
import com.smartpresence.service.JustificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JustificationServiceImpl implements JustificationService {

    private final JustificationAbsenceRepository justificationAbsenceRepository;
    private final EtudiantRepository etudiantRepository;
    private final SeanceRepository seanceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JustificationAbsenceMapper justificationAbsenceMapper;

    @Override
    @Transactional(readOnly = true)
    public List<JustificationAbsenceResponse> findAll() {
        return justificationAbsenceMapper.toResponseList(justificationAbsenceRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JustificationAbsenceResponse> findByEtudiant(UUID etudiantId) {
        return justificationAbsenceMapper.toResponseList(
                justificationAbsenceRepository.findByEtudiantId(etudiantId));
    }

    @Override
    @Transactional
    public JustificationAbsenceResponse create(UUID etudiantId, String motif, LocalDate dateAbsence, UUID seanceId) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant", "id", etudiantId));

        JustificationAbsence justification = new JustificationAbsence();
        justification.setEtudiant(etudiant);
        justification.setMotif(motif);
        justification.setDateAbsence(dateAbsence);
        if (seanceId != null) {
            Seance seance = seanceRepository.findById(seanceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Séance", "id", seanceId));
            justification.setSeance(seance);
        }
        justification.setStatut(StatutJustification.EN_ATTENTE);

        return justificationAbsenceMapper.toResponse(justificationAbsenceRepository.save(justification));
    }

    @Override
    @Transactional
    public void traiter(UUID id, StatutJustification statut, UUID traiteParId, String commentaire) {
        JustificationAbsence justification = justificationAbsenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Justification", "id", id));

        Utilisateur traitePar = utilisateurRepository.findById(traiteParId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", traiteParId));

        justification.setStatut(statut);
        justification.setTraitePar(traitePar);
        justification.setCommentaireTraitement(commentaire);
        justificationAbsenceRepository.save(justification);
    }
}
