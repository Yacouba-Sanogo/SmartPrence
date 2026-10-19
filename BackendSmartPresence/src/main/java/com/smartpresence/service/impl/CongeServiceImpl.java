package com.smartpresence.service.impl;

import com.smartpresence.constants.StatutDemande;
import com.smartpresence.constants.TypeConge;
import com.smartpresence.constants.TypeNotification;
import com.smartpresence.dto.request.GestionRequests;
import com.smartpresence.dto.response.GestionResponses;
import com.smartpresence.entity.DemandeConge;
import com.smartpresence.entity.Personnel;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.repository.DemandeCongeRepository;
import com.smartpresence.repository.SeanceRepository;
import com.smartpresence.service.CongeService;
import com.smartpresence.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CongeServiceImpl implements CongeService {

    private static final int DUREE_MAX_JOURS = 365;

    private final EnseignantDuCompte enseignants;
    private final DemandeCongeRepository repository;
    private final SeanceRepository seanceRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<GestionResponses.Conge> mesDemandes(UUID utilisateurId) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        return repository.findByEnseignantIdOrderByDebutDesc(enseignant.getId()).stream()
                .map(this::versReponse)
                .toList();
    }

    @Override
    @Transactional
    public GestionResponses.Conge demander(UUID utilisateurId, GestionRequests.Conge requete) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        if (requete.fin().isBefore(requete.debut())) {
            throw new BusinessException("La date de fin précède la date de début");
        }
        if (ChronoUnit.DAYS.between(requete.debut(), requete.fin()) >= DUREE_MAX_JOURS) {
            throw new BusinessException("Une demande ne peut pas dépasser un an");
        }
        if (!repository.findRecouvrant(enseignant.getId(), requete.debut(), requete.fin()).isEmpty()) {
            throw new BusinessException(
                    "Une demande en attente ou acceptée couvre déjà une partie de cette période",
                    HttpStatus.CONFLICT);
        }

        DemandeConge demande = new DemandeConge();
        demande.setEnseignant(enseignant);
        demande.setType(requete.type() == null ? TypeConge.CONGE : requete.type());
        demande.setDebut(requete.debut());
        demande.setFin(requete.fin());
        demande.setMotif(requete.motif().trim());
        DemandeConge enregistree = repository.save(demande);
        log.info("Demande de {} — enseignant={}, du {} au {}", enregistree.getType(),
                enseignant.getMatricule(), enregistree.getDebut(), enregistree.getFin());
        return versReponse(enregistree);
    }

    @Override
    @Transactional
    public GestionResponses.Conge annuler(UUID utilisateurId, UUID demandeId) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        DemandeConge demande = repository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", demandeId));
        if (!demande.getEnseignant().getId().equals(enseignant.getId())) {
            throw new BusinessException("Cette demande n'est pas la vôtre", HttpStatus.FORBIDDEN);
        }
        if (demande.getStatut() != StatutDemande.EN_ATTENTE) {
            throw new BusinessException(
                    "Seule une demande en attente peut être retirée", HttpStatus.CONFLICT);
        }
        demande.setStatut(StatutDemande.ANNULEE);
        return versReponse(repository.save(demande));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GestionResponses.Conge> aTraiter(StatutDemande statut) {
        return repository.findParStatut(statut).stream().map(this::versReponse).toList();
    }

    @Override
    @Transactional
    public GestionResponses.Conge decider(UUID demandeId, GestionRequests.DecisionConge decision) {
        DemandeConge demande = repository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", demandeId));
        if (demande.getStatut() != StatutDemande.EN_ATTENTE) {
            throw new BusinessException("Cette demande a déjà été traitée", HttpStatus.CONFLICT);
        }
        String reponse = decision.reponse() == null || decision.reponse().isBlank()
                ? null : decision.reponse().trim();
        // Un refus sans motif laisserait l'enseignant sans rien à contester ni à corriger.
        if (!decision.accepter() && reponse == null) {
            throw new BusinessException("Précisez le motif du refus");
        }

        demande.setStatut(decision.accepter() ? StatutDemande.ACCEPTEE : StatutDemande.REFUSEE);
        demande.setReponse(reponse);
        demande.setTraiteeLe(Instant.now());
        DemandeConge traitee = repository.save(demande);

        notificationService.notifierEnseignant(traitee.getEnseignant(), TypeNotification.INFORMATION,
                decision.accepter() ? "Demande acceptée" : "Demande refusée",
                "Votre demande du " + traitee.getDebut() + " au " + traitee.getFin()
                        + (decision.accepter() ? " est acceptée." : " est refusée.")
                        + (reponse == null ? "" : " " + reponse));
        return versReponse(traitee);
    }

    private GestionResponses.Conge versReponse(DemandeConge d) {
        ZoneId zone = ZoneId.systemDefault();
        Instant de = d.getDebut().atStartOfDay(zone).toInstant();
        Instant a = d.getFin().plusDays(1).atStartOfDay(zone).toInstant();
        int seances = (int) seanceRepository.findParEnseignant(d.getEnseignant().getId(), de, a).stream()
                .filter(s -> s.getStatut() != com.smartpresence.constants.StatutSeance.ANNULEE)
                .count();
        Personnel e = d.getEnseignant();
        return new GestionResponses.Conge(d.getId(), e.getId(), (e.getPrenom() + " " + e.getNom()).trim(),
                d.getType(), d.getDebut(), d.getFin(), d.getMotif(), d.getStatut(), d.getReponse(),
                d.getTraiteeLe(), seances, d.getCreatedAt());
    }
}
