package com.smartpresence.service.impl;

import com.smartpresence.constants.SourcePresence;
import com.smartpresence.constants.StatutPresence;
import com.smartpresence.constants.StatutSignalement;
import com.smartpresence.constants.TypePersonnel;
import com.smartpresence.constants.TypeSignalement;
import com.smartpresence.dto.request.SignalementRequest;
import com.smartpresence.dto.request.TraitementSignalementRequest;
import com.smartpresence.dto.response.SignalementResponse;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.entity.Personnel;
import com.smartpresence.entity.Presence;
import com.smartpresence.entity.Seance;
import com.smartpresence.entity.SignalementPresence;
import com.smartpresence.entity.Utilisateur;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.PersonnelRepository;
import com.smartpresence.repository.PresenceRepository;
import com.smartpresence.repository.SeanceRepository;
import com.smartpresence.repository.SignalementRepository;
import com.smartpresence.repository.UtilisateurRepository;
import com.smartpresence.service.SignalementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalementServiceImpl implements SignalementService {

    private final SignalementRepository signalementRepository;
    private final SeanceRepository seanceRepository;
    private final PersonnelRepository personnelRepository;
    private final EtudiantRepository etudiantRepository;
    private final PresenceRepository presenceRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional
    public SignalementResponse signaler(UUID utilisateurId, UUID seanceId,
                                        SignalementRequest request) {
        Personnel enseignant = enseignantDuCompte(utilisateurId);
        Seance seance = seanceRepository.findAvecContexte(seanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Séance", "id", seanceId));

        if (!seance.getEnseignant().getId().equals(enseignant.getId())) {
            throw new BusinessException(
                    "Cette séance n'est pas la vôtre", HttpStatus.FORBIDDEN);
        }

        SignalementPresence signalement = new SignalementPresence();
        signalement.setSeance(seance);
        signalement.setEnseignant(enseignant);
        signalement.setType(request.getType());
        signalement.setDescription(request.getDescription().trim());

        if (request.getType() == TypeSignalement.ETUDIANT_NON_RECONNU) {
            if (request.getEtudiantId() == null) {
                throw new BusinessException(
                        "Précisez l'étudiant concerné : sans lui, la scolarité ne saurait "
                                + "pas qui régulariser");
            }
            signalement.setEtudiant(etudiantDeLaClasse(request.getEtudiantId(), seance));
        }

        SignalementPresence enregistre = signalementRepository.save(signalement);
        log.info("Signalement déposé — séance={}, enseignant={}, type={}",
                seanceId, enseignant.getMatricule(), request.getType());
        return versReponse(enregistre);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignalementResponse> mesSignalements(UUID utilisateurId) {
        Personnel enseignant = enseignantDuCompte(utilisateurId);
        return signalementRepository.findParEnseignant(enseignant.getId()).stream()
                .map(this::versReponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignalementResponse> rechercher(StatutSignalement statut) {
        return signalementRepository.rechercher(statut).stream()
                .map(this::versReponse)
                .toList();
    }

    @Override
    @Transactional
    public SignalementResponse traiter(UUID signalementId, UUID arbitreId,
                                       TraitementSignalementRequest request) {
        SignalementPresence signalement = signalementRepository.findAvecContexte(signalementId)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement", "id", signalementId));

        if (!signalement.estEnAttente()) {
            throw new BusinessException(
                    "Ce signalement a déjà été traité", HttpStatus.CONFLICT);
        }

        Utilisateur arbitre = utilisateurRepository.findById(arbitreId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", arbitreId));

        signalement.setTraitePar(arbitre);
        signalement.setCommentaireTraitement(request.getCommentaire().trim());
        signalement.setTraiteLe(Instant.now());

        if (Boolean.TRUE.equals(request.getAccepte())) {
            signalement.setStatut(StatutSignalement.ACCEPTE);
            if (signalement.getType() == TypeSignalement.ETUDIANT_NON_RECONNU) {
                signalement.setPresenceCorrective(
                        creerReleveCorrectif(signalement, request.getHeurePresence()));
            }
        } else {
            signalement.setStatut(StatutSignalement.REJETE);
        }

        log.info("Signalement {} — décision={}, arbitre={}",
                signalementId, signalement.getStatut(), arbitre.getEmail());
        return versReponse(signalementRepository.save(signalement));
    }

    /**
     * Produit le relevé correctif d'un signalement accepté.
     *
     * <p>Source {@code MANUEL} et non {@code ESP32} : la distinction doit survivre à
     * l'arbitrage, sans quoi une présence décidée par un humain deviendrait
     * indiscernable d'une identification par le capteur.</p>
     */
    private Presence creerReleveCorrectif(SignalementPresence signalement, LocalTime heureDemandee) {
        Seance seance = signalement.getSeance();
        Etudiant etudiant = signalement.getEtudiant();

        ZoneId zone = ZoneId.systemDefault();
        LocalDate dateSeance = seance.getDebut().atZone(zone).toLocalDate();
        LocalTime heure = heureDemandee != null
                ? heureDemandee
                : seance.getDebut().atZone(zone).toLocalTime().withNano(0);

        // Un relevé peut déjà exister si le capteur a fini par identifier l'étudiant
        // entre le signalement et l'arbitrage : dans ce cas, rien à corriger.
        boolean dejaReleve = presenceRepository.findBySeanceId(seance.getId()).stream()
                .anyMatch(p -> p.getEtudiant().getId().equals(etudiant.getId()));
        if (dejaReleve) {
            throw new BusinessException(
                    "Un relevé existe déjà pour cet étudiant sur cette séance : "
                            + "le signalement n'a plus lieu d'être", HttpStatus.CONFLICT);
        }

        Instant maintenant = Instant.now();
        Presence presence = new Presence();
        presence.setEtudiant(etudiant);
        presence.setSeance(seance);
        presence.setDatePresence(dateSeance);
        presence.setHeurePresence(heure);
        presence.setStatut(StatutPresence.PRESENT);
        presence.setSource(SourcePresence.MANUEL);
        presence.setCreeLeDevice(maintenant);
        presence.setSynchroniseLe(maintenant);
        return presenceRepository.save(presence);
    }

    /** Vérifie que l'étudiant signalé appartient bien à la classe de la séance. */
    private Etudiant etudiantDeLaClasse(UUID etudiantId, Seance seance) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant", "id", etudiantId));
        if (!etudiant.getClasse().getId().equals(seance.getClasse().getId())) {
            throw new BusinessException(
                    "Cet étudiant n'appartient pas à la classe de la séance", HttpStatus.CONFLICT);
        }
        return etudiant;
    }

    private Personnel enseignantDuCompte(UUID utilisateurId) {
        Personnel personnel = personnelRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new BusinessException(
                        "Ce compte n'est rattaché à aucune fiche d'agent", HttpStatus.FORBIDDEN));
        if (personnel.getType() != TypePersonnel.ENSEIGNANT) {
            throw new BusinessException(
                    "Ce compte n'est pas rattaché à un enseignant", HttpStatus.FORBIDDEN);
        }
        return personnel;
    }

    private SignalementResponse versReponse(SignalementPresence s) {
        Etudiant etudiant = s.getEtudiant();
        return SignalementResponse.builder()
                .id(s.getId())
                .seanceId(s.getSeance().getId())
                .matiereLibelle(s.getSeance().getMatiere().getLibelle())
                .classeCode(s.getSeance().getClasse().getCode())
                .seanceDebut(s.getSeance().getDebut())
                .enseignantId(s.getEnseignant().getId())
                .enseignantNom(s.getEnseignant().getPrenom() + " " + s.getEnseignant().getNom())
                .etudiantId(etudiant == null ? null : etudiant.getId())
                .etudiantNom(etudiant == null ? null : etudiant.getPrenom() + " " + etudiant.getNom())
                .etudiantMatricule(etudiant == null ? null : etudiant.getMatricule())
                .type(s.getType())
                .description(s.getDescription())
                .statut(s.getStatut())
                .commentaireTraitement(s.getCommentaireTraitement())
                .traiteLe(s.getTraiteLe())
                .presenceCorrectiveId(
                        s.getPresenceCorrective() == null ? null : s.getPresenceCorrective().getId())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
