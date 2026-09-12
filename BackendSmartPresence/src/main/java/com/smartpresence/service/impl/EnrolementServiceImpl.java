package com.smartpresence.service.impl;

import com.smartpresence.constants.StatutDemandeEnrolement;
import com.smartpresence.dto.response.DemandeEnrolementResponse;
import com.smartpresence.dto.response.EnrolementAServirResponse;
import com.smartpresence.entity.DemandeEnrolement;
import com.smartpresence.entity.Device;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.repository.DemandeEnrolementRepository;
import com.smartpresence.repository.DeviceRepository;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.service.EnrolementService;
import com.smartpresence.service.EtudiantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Enrôlement biométrique en libre-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrolementServiceImpl implements EnrolementService {

    /**
     * Durée de vie d'une demande.
     *
     * <p>Assez longue pour traverser un couloir et faire la queue, assez courte pour
     * qu'une demande oubliée ne traîne pas dans la file jusqu'au lendemain.</p>
     */
    private static final Duration VALIDITE = Duration.ofMinutes(10);

    /**
     * Temps pendant lequel un lecteur garde pour lui la demande qu'il affiche.
     *
     * <p>Au-delà, on considère que l'étudiant ne s'est pas présenté et la demande
     * retourne dans la file : un lecteur en panne ne doit pas retenir indéfiniment
     * quelqu'un qui attend devant un autre capteur.</p>
     */
    private static final Duration RESERVATION = Duration.ofMinutes(2);

    private static final int TENTATIVES_CODE = 10;

    private final DemandeEnrolementRepository demandeRepository;
    private final EtudiantRepository etudiantRepository;
    private final DeviceRepository deviceRepository;
    private final EtudiantService etudiantService;

    private final SecureRandom alea = new SecureRandom();

    // ------------------------------------------------------------------
    // Côté étudiant
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public DemandeEnrolementResponse demander(UUID utilisateurId) {
        Etudiant etudiant = etudiantDe(utilisateurId);
        Instant maintenant = Instant.now();

        if (!etudiant.isActif()) {
            throw new BusinessException(
                    "Votre dossier est inactif : rapprochez-vous de la scolarité",
                    HttpStatus.CONFLICT);
        }
        if (etudiant.isEnrole()) {
            // Se réenrôler soi-même laisserait derrière chaque tentative un emplacement
            // occupé pour rien dans la mémoire du capteur, que personne ne viendrait
            // jamais libérer. La révocation reste donc un geste de la scolarité.
            throw new BusinessException(
                    "Votre empreinte est déjà enregistrée. Pour la remplacer, "
                            + "demandez à la scolarité de la révoquer d'abord.",
                    HttpStatus.CONFLICT);
        }

        List<DemandeEnrolement> ouvertes = demandeRepository.ouvertesDe(etudiant.getId(), maintenant);
        if (!ouvertes.isEmpty()) {
            return reponse(ouvertes.get(0), etudiant, maintenant);
        }

        DemandeEnrolement demande = new DemandeEnrolement();
        demande.setEtudiant(etudiant);
        demande.setCode(codeDisponible());
        demande.setStatut(StatutDemandeEnrolement.EN_ATTENTE);
        demande.setExpireLe(maintenant.plus(VALIDITE));
        demande = demandeRepository.save(demande);

        log.info("Demande d'enrôlement ouverte — matricule={}, code={}",
                etudiant.getMatricule(), demande.getCode());
        return reponse(demande, etudiant, maintenant);
    }

    @Override
    @Transactional
    public DemandeEnrolementResponse maDemande(UUID utilisateurId) {
        Etudiant etudiant = etudiantDe(utilisateurId);
        Instant maintenant = Instant.now();
        clore(maintenant);

        return demandeRepository.findFirstByEtudiantIdOrderByCreatedAtDesc(etudiant.getId())
                .map(demande -> reponse(demande, etudiant, maintenant))
                .orElseGet(() -> DemandeEnrolementResponse.builder()
                        .enrole(etudiant.isEnrole())
                        .build());
    }

    @Override
    @Transactional
    public DemandeEnrolementResponse annuler(UUID utilisateurId) {
        Etudiant etudiant = etudiantDe(utilisateurId);
        Instant maintenant = Instant.now();

        List<DemandeEnrolement> ouvertes = demandeRepository.ouvertesDe(etudiant.getId(), maintenant);
        if (ouvertes.isEmpty()) {
            throw new BusinessException("Vous n'avez aucune demande d'enrôlement en cours",
                    HttpStatus.CONFLICT);
        }

        DemandeEnrolement demande = ouvertes.get(0);
        demande.setStatut(StatutDemandeEnrolement.ANNULEE);
        demande.setDateTraitement(maintenant);
        return reponse(demandeRepository.save(demande), etudiant, maintenant);
    }

    // ------------------------------------------------------------------
    // Côté lecteur
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public Optional<EnrolementAServirResponse> prochaine(UUID deviceId) {
        Instant maintenant = Instant.now();
        clore(maintenant);

        Optional<DemandeEnrolement> aServir = demandeRepository.fileDAttente(maintenant).stream()
                .filter(demande -> disponiblePour(demande, deviceId, maintenant))
                .findFirst();

        if (aServir.isEmpty()) {
            return Optional.empty();
        }

        DemandeEnrolement demande = aServir.get();
        demande.setDevice(deviceRepository.findById(deviceId).orElse(null));
        demande.setDateReservation(maintenant);
        demandeRepository.save(demande);

        Etudiant etudiant = demande.getEtudiant();
        return Optional.of(EnrolementAServirResponse.builder()
                .code(demande.getCode())
                .nomAffiche(nomAffiche(etudiant))
                .matricule(etudiant.getMatricule())
                .secondesRestantes(secondesRestantes(demande, maintenant))
                .build());
    }

    @Override
    @Transactional
    public DemandeEnrolementResponse enregistrer(UUID deviceId, String code, String reference) {
        Instant maintenant = Instant.now();
        String recherche = code == null ? "" : code.trim();

        DemandeEnrolement demande = demandeRepository.ouvertesParCode(recherche, maintenant).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Aucune demande d'enrôlement en cours pour le code " + recherche
                                + " — elle a peut-être expiré",
                        HttpStatus.NOT_FOUND));

        if (!disponiblePour(demande, deviceId, maintenant)) {
            throw new BusinessException(
                    "Cette demande est prise en charge par un autre lecteur",
                    HttpStatus.CONFLICT);
        }

        Etudiant etudiant = demande.getEtudiant();
        // Délégué : les contrôles d'unicité de la référence et d'activité du dossier
        // valent pour tout enrôlement, qu'il vienne d'un agent ou du libre-service.
        etudiantService.enroler(etudiant.getId(), reference);

        demande.setStatut(StatutDemandeEnrolement.TERMINEE);
        demande.setBiometricId(reference.trim());
        demande.setDateTraitement(maintenant);
        demande.setDevice(deviceRepository.findById(deviceId).orElse(null));
        demande = demandeRepository.save(demande);

        log.info("Enrôlement libre-service abouti — matricule={}, code={}, référence={}",
                etudiant.getMatricule(), demande.getCode(), reference);

        // Relu depuis la base : « enrôlé » doit refléter l'écriture qui vient d'avoir lieu.
        Etudiant rafraichi = etudiantRepository.findById(etudiant.getId()).orElse(etudiant);
        return reponse(demande, rafraichi, maintenant);
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------

    /**
     * Une demande est disponible pour un lecteur si personne ne l'a prise, si c'est
     * ce lecteur qui l'a prise, ou si la prise de l'autre a fait long feu.
     */
    private boolean disponiblePour(DemandeEnrolement demande, UUID deviceId, Instant maintenant) {
        if (demande.getDateReservation() == null || demande.getDevice() == null) {
            return true;
        }
        if (demande.getDevice().getId().equals(deviceId)) {
            return true;
        }
        return demande.getDateReservation().plus(RESERVATION).isBefore(maintenant);
    }

    /**
     * Clôt les demandes que le temps a rattrapées.
     *
     * <p>Fait à la lecture plutôt que par une tâche périodique : une demande périmée
     * n'a d'effet sur rien tant que personne ne regarde la file, et l'application n'a
     * pas besoin d'un ordonnanceur pour une échéance de dix minutes.</p>
     */
    private void clore(Instant maintenant) {
        List<DemandeEnrolement> perimees = demandeRepository.perimees(maintenant);
        if (perimees.isEmpty()) {
            return;
        }
        perimees.forEach(demande -> {
            demande.setStatut(StatutDemandeEnrolement.EXPIREE);
            demande.setDateTraitement(maintenant);
        });
        demandeRepository.saveAll(perimees);
    }

    /** Code à six chiffres, non attribué à une autre demande en attente. */
    private String codeDisponible() {
        for (int essai = 0; essai < TENTATIVES_CODE; essai++) {
            String code = String.format("%06d", alea.nextInt(1_000_000));
            if (!demandeRepository.existsByCodeAndStatut(code, StatutDemandeEnrolement.EN_ATTENTE)) {
                return code;
            }
        }
        throw new BusinessException(
                "Trop de demandes d'enrôlement en cours — réessayez dans un instant",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    private Etudiant etudiantDe(UUID utilisateurId) {
        return etudiantRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Étudiant", "utilisateurId", utilisateurId));
    }

    private String nomAffiche(Etudiant etudiant) {
        return (etudiant.getNom() + " " + etudiant.getPrenom()).trim();
    }

    private long secondesRestantes(DemandeEnrolement demande, Instant maintenant) {
        if (demande.getStatut() != StatutDemandeEnrolement.EN_ATTENTE) {
            return 0L;
        }
        return Math.max(0L, Duration.between(maintenant, demande.getExpireLe()).toSeconds());
    }

    private DemandeEnrolementResponse reponse(DemandeEnrolement demande, Etudiant etudiant,
                                              Instant maintenant) {
        boolean ouverte = demande.getStatut() == StatutDemandeEnrolement.EN_ATTENTE;
        Device lecteur = demande.getDevice();
        return DemandeEnrolementResponse.builder()
                .code(ouverte ? demande.getCode() : null)
                .statut(demande.getStatut())
                .expireLe(demande.getExpireLe())
                .secondesRestantes(secondesRestantes(demande, maintenant))
                .lecteur(lecteur == null ? null : lecteur.getNom())
                .enrole(etudiant.isEnrole())
                .build();
    }
}
