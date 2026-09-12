package com.smartpresence.service.impl;

import com.smartpresence.constants.StatutSeance;
import com.smartpresence.constants.TypePersonnel;
import com.smartpresence.dto.request.MatiereRequest;
import com.smartpresence.dto.request.SeanceRequest;
import com.smartpresence.dto.response.MatiereResponse;
import com.smartpresence.dto.response.SeanceResponse;
import com.smartpresence.entity.Matiere;
import com.smartpresence.entity.UniteEnseignement;
import com.smartpresence.entity.Personnel;
import com.smartpresence.entity.Salle;
import com.smartpresence.entity.Seance;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.MatiereRepository;
import com.smartpresence.repository.PersonnelRepository;
import com.smartpresence.repository.PresenceRepository;
import com.smartpresence.repository.SalleRepository;
import com.smartpresence.repository.SeanceRepository;
import com.smartpresence.repository.UniteEnseignementRepository;
import com.smartpresence.service.AcademicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Référentiel des matières et planification des séances.
 *
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicServiceImpl implements AcademicService {

    private final PersonnelRepository personnelRepository;
    private final MatiereRepository matiereRepository;
    private final UniteEnseignementRepository uniteEnseignementRepository;
    private final SeanceRepository seanceRepository;
    private final ClasseRepository classeRepository;
    private final SalleRepository salleRepository;
    private final PresenceRepository presenceRepository;

    // ------------------------------------------------------------------
    // Matières
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<MatiereResponse> matieres() {
        return matiereRepository.findAll().stream()
                .sorted((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()))
                .map(this::versReponse)
                .toList();
    }

    @Override
    @Transactional
    public MatiereResponse createMatiere(MatiereRequest request) {
        if (matiereRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Ce code matière existe déjà", HttpStatus.CONFLICT);
        }
        Matiere matiere = new Matiere();
        appliquer(matiere, request);
        return versReponse(matiereRepository.save(matiere));
    }

    @Override
    @Transactional
    public MatiereResponse updateMatiere(Long id, MatiereRequest request) {
        Matiere matiere = matiere(id);
        if (!matiere.getCode().equals(request.getCode())
                && matiereRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Ce code matière existe déjà", HttpStatus.CONFLICT);
        }
        appliquer(matiere, request);
        return versReponse(matiereRepository.save(matiere));
    }

    @Override
    @Transactional
    public void deleteMatiere(Long id) {
        Matiere matiere = matiere(id);

        // Supprimer en cascade emporterait les séances, et avec elles les relevés de
        // présence qui s'y rattachent. Désactiver la matière est le geste attendu.
        long seances = seanceRepository.countByMatiereId(matiere.getId());
        if (seances > 0) {
            throw new BusinessException(
                    "Cette matière est utilisée par " + seances + " séance(s). "
                            + "Désactivez-la plutôt que de la supprimer.", HttpStatus.CONFLICT);
        }

        matiereRepository.delete(matiere);
        log.info("Matière supprimée — code={}", matiere.getCode());
    }

    // ------------------------------------------------------------------
    // Séances
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<SeanceResponse> seances(LocalDate debut, LocalDate fin,
                                        Long classeId, UUID enseignantId) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate premierJour = debut != null ? debut : LocalDate.now();
        LocalDate dernierJour = fin != null ? fin : premierJour;
        if (dernierJour.isBefore(premierJour)) {
            throw new BusinessException("La fin de période précède son début");
        }

        return seanceRepository.rechercher(
                        premierJour.atStartOfDay(zone).toInstant(),
                        dernierJour.plusDays(1).atStartOfDay(zone).toInstant(),
                        classeId, enseignantId).stream()
                .map(this::versReponse)
                .toList();
    }

    @Override
    @Transactional
    public SeanceResponse createSeance(SeanceRequest request) {
        Seance seance = new Seance();
        appliquer(seance, request, null);
        Seance enregistree = seanceRepository.save(seance);
        log.info("Séance planifiée — classe={}, matière={}, début={}",
                enregistree.getClasse().getCode(), enregistree.getMatiere().getCode(),
                enregistree.getDebut());
        return versReponse(enregistree);
    }

    @Override
    @Transactional
    public SeanceResponse updateSeance(UUID id, SeanceRequest request) {
        Seance seance = seanceRepository.findAvecContexte(id)
                .orElseThrow(() -> new ResourceNotFoundException("Séance", "id", id));

        // Un relevé de présence est daté par rapport au créneau de sa séance. Déplacer
        // une séance déjà relevée transformerait rétroactivement des présences en
        // retards, ou l'inverse : la modification est refusée, l'annulation reste ouverte.
        if (!presenceRepository.findBySeanceId(id).isEmpty()) {
            throw new BusinessException(
                    "Des présences sont déjà relevées sur cette séance : elle ne peut plus "
                            + "être modifiée. Annulez-la si elle n'a pas eu lieu.",
                    HttpStatus.CONFLICT);
        }

        appliquer(seance, request, id);
        return versReponse(seanceRepository.save(seance));
    }

    @Override
    @Transactional
    public void deleteSeance(UUID id) {
        Seance seance = seanceRepository.findAvecContexte(id)
                .orElseThrow(() -> new ResourceNotFoundException("Séance", "id", id));

        if (!presenceRepository.findBySeanceId(id).isEmpty()) {
            throw new BusinessException(
                    "Des présences sont rattachées à cette séance : la supprimer effacerait "
                            + "des relevés. Annulez-la plutôt.", HttpStatus.CONFLICT);
        }

        seanceRepository.delete(seance);
        log.info("Séance supprimée — id={}", id);
    }

    @Override
    @Transactional
    public void changeStatutSeance(UUID id, String statut) {
        Seance seance = seanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Séance", "id", id));
        try {
            seance.setStatut(StatutSeance.valueOf(statut));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Statut de séance invalide : " + statut);
        }
        seanceRepository.save(seance);
    }

    // ------------------------------------------------------------------

    /**
     * Renseigne une séance depuis la requête, après avoir vérifié sa cohérence.
     *
     * @param exclusion séance en cours de modification, exclue de la détection de
     *                  conflit pour qu'elle ne se voie pas elle-même
     */
    private void appliquer(Seance seance, SeanceRequest request, UUID exclusion) {
        if (!request.getFin().isAfter(request.getDebut())) {
            throw new BusinessException("La fin de séance doit être postérieure à son début");
        }

        Personnel enseignant = personnelRepository.findById(request.getEnseignantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Personnel", "id", request.getEnseignantId()));
        if (enseignant.getType() != TypePersonnel.ENSEIGNANT) {
            throw new BusinessException("Cet agent n'est pas un enseignant");
        }
        if (!enseignant.isActif()) {
            throw new BusinessException("Cet enseignant est inactif");
        }

        Salle salle = request.getSalleId() == null ? null
                : salleRepository.findById(request.getSalleId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Salle", "id", request.getSalleId()));

        assertLibre(request, enseignant, salle, exclusion);

        seance.setClasse(classeRepository.findById(request.getClasseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Classe", "id", request.getClasseId())));
        seance.setMatiere(matiere(request.getMatiereId()));
        seance.setEnseignant(enseignant);
        seance.setSalle(salle);
        seance.setDebut(request.getDebut());
        seance.setFin(request.getFin());
        seance.setNote(request.getNote());
    }

    /**
     * Refuse un créneau déjà occupé.
     *
     * <p>Une classe ne peut pas suivre deux cours à la fois, un enseignant ne peut pas
     * en assurer deux, et une salle ne peut pas en accueillir deux. Sans ce contrôle,
     * l'emploi du temps accepte des situations impossibles, et le lecteur de la salle
     * attribuerait les passages à la mauvaise séance.</p>
     */
    private void assertLibre(SeanceRequest request, Personnel enseignant,
                             Salle salle, UUID exclusion) {
        List<Seance> conflits = seanceRepository.chevauchements(
                request.getDebut(), request.getFin(), request.getClasseId(),
                enseignant.getId(), salle == null ? null : salle.getId(), exclusion);

        if (conflits.isEmpty()) {
            return;
        }

        String detail = conflits.stream()
                .limit(3)
                .map(this::motifDuConflit)
                .distinct()
                .collect(Collectors.joining(" ; "));
        throw new BusinessException("Créneau déjà occupé — " + detail, HttpStatus.CONFLICT);
    }

    private String motifDuConflit(Seance conflit) {
        return "%s assure %s pour %s".formatted(
                conflit.getEnseignant().getPrenom() + " " + conflit.getEnseignant().getNom(),
                conflit.getMatiere().getLibelle(),
                conflit.getClasse().getCode());
    }

    private void appliquer(Matiere matiere, MatiereRequest request) {
        matiere.setCode(request.getCode().trim().toUpperCase());
        matiere.setLibelle(request.getLibelle().trim());
        matiere.setCredits(request.getCredits());
        matiere.setDescription(request.getDescription());
        matiere.setUniteEnseignement(uniteDemandee(request.getUniteEnseignementId()));
        if (request.getActive() != null) {
            matiere.setActive(request.getActive());
        }
    }

    /**
     * UE de rattachement, ou {@code null}.
     *
     * <p>Le détacher est un geste légitime — une matière sort d'une maquette — et se
     * fait en omettant simplement l'identifiant.</p>
     */
    private UniteEnseignement uniteDemandee(Long uniteId) {
        if (uniteId == null) {
            return null;
        }
        return uniteEnseignementRepository.findById(uniteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unité d'enseignement", "id", uniteId));
    }

    private Matiere matiere(Long id) {
        return matiereRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matière", "id", id));
    }

    private MatiereResponse versReponse(Matiere m) {
        return MatiereResponse.builder()
                .id(m.getId())
                .code(m.getCode())
                .libelle(m.getLibelle())
                .credits(m.getCredits())
                .description(m.getDescription())
                .active(m.isActive())
                .uniteEnseignementId(m.getUniteEnseignement() == null
                        ? null : m.getUniteEnseignement().getId())
                .uniteEnseignementCode(m.getUniteEnseignement() == null
                        ? null : m.getUniteEnseignement().getCode())
                .uniteEnseignementLibelle(m.getUniteEnseignement() == null
                        ? null : m.getUniteEnseignement().getLibelle())
                .build();
    }

    private SeanceResponse versReponse(Seance s) {
        return SeanceResponse.builder()
                .id(s.getId())
                .classeId(s.getClasse().getId())
                .classeCode(s.getClasse().getCode())
                .matiereId(s.getMatiere().getId())
                .matiereLibelle(s.getMatiere().getLibelle())
                .enseignantId(s.getEnseignant().getId())
                .enseignantNom(s.getEnseignant().getPrenom() + " " + s.getEnseignant().getNom())
                .salleId(s.getSalle() == null ? null : s.getSalle().getId())
                .salleLibelle(s.getSalle() == null ? null : s.getSalle().getLibelle())
                .debut(s.getDebut())
                .fin(s.getFin())
                .statut(s.getStatut())
                .note(s.getNote())
                .build();
    }
}
