package com.smartpresence.service.impl;

import com.smartpresence.constants.SourcePresence;
import com.smartpresence.constants.StatutSynchronisation;
import com.smartpresence.constants.UsageDevice;
import com.smartpresence.dto.request.PresenceESP32Request;
import com.smartpresence.dto.request.PresenceItemRequest;
import com.smartpresence.dto.request.PresenceManuelRequest;
import com.smartpresence.dto.request.PresenceSearchCriteria;
import com.smartpresence.dto.response.PagedResponse;
import com.smartpresence.dto.response.PresenceResponse;
import com.smartpresence.dto.response.SyncSummaryResponse;
import com.smartpresence.entity.Device;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.entity.HistoriqueSynchronisation;
import com.smartpresence.entity.Presence;
import com.smartpresence.entity.Seance;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.PresenceMapper;
import com.smartpresence.repository.DeviceRepository;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.HistoriqueSynchronisationRepository;
import com.smartpresence.repository.PresenceRepository;
import com.smartpresence.repository.PresenceSpecification;
import com.smartpresence.repository.SeanceRepository;
import com.smartpresence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PresenceRepository presenceRepository;
    private final EtudiantRepository etudiantRepository;
    private final DeviceRepository deviceRepository;
    private final HistoriqueSynchronisationRepository historiqueRepository;
    private final SeanceRepository seanceRepository;
    private final PresenceMapper presenceMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PresenceResponse> search(PresenceSearchCriteria criteria) {
        if (criteria.getDateDebut() != null && criteria.getDateFin() != null
                && criteria.getDateDebut().isAfter(criteria.getDateFin())) {
            throw new BusinessException("La date de début doit être antérieure ou égale à la date de fin");
        }
        int page = Math.max(0, criteria.getPage());
        int size = Math.min(MAX_PAGE_SIZE, Math.max(1, criteria.getSize()));
        Page<Presence> result = presenceRepository.findAll(
                PresenceSpecification.withCriteria(criteria),
                PageRequest.of(page, size, Sort.by("datePresence").descending().and(Sort.by("heurePresence").descending())));
        return PagedResponse.<PresenceResponse>builder()
                .content(presenceMapper.toResponseList(result.getContent()))
                .page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages()).last(result.isLast())
                .build();
    }

    @Override
    @Transactional
    public PresenceResponse createManual(PresenceManuelRequest request) {
        Etudiant etudiant = getActiveStudent(request.getEtudiantId());
        Device device = request.getDeviceId() == null ? null : getDevice(request.getDeviceId());
        if (presenceRepository.existsByDeviceIdAndEtudiantIdAndDatePresenceAndHeurePresence(
                request.getDeviceId(), request.getEtudiantId(), request.getDatePresence(), request.getHeurePresence())) {
            throw new BusinessException("Cette présence existe déjà", HttpStatus.CONFLICT);
        }
        Instant now = Instant.now();
        Presence presence = new Presence();
        presence.setEtudiant(etudiant);
        presence.setDevice(device);
        if (request.getSeanceId() != null) {
            var seance = seanceRepository.findById(request.getSeanceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Séance", "id", request.getSeanceId()));
            if (!seance.getClasse().getId().equals(etudiant.getClasse().getId())) {
                throw new BusinessException("L'étudiant n'appartient pas à la classe de cette séance", HttpStatus.CONFLICT);
            }
            presence.setSeance(seance);
        }
        presence.setDatePresence(request.getDatePresence());
        presence.setHeurePresence(request.getHeurePresence());
        presence.setStatut(request.getStatut());
        presence.setSource(SourcePresence.MANUEL);
        presence.setCreeLeDevice(now);
        presence.setSynchroniseLe(now);
        return presenceMapper.toResponse(presenceRepository.save(presence));
    }

    @Override
    @Transactional
    public SyncSummaryResponse synchronize(UUID authenticatedDeviceId, PresenceESP32Request request) {
        if (!authenticatedDeviceId.equals(request.getDeviceId())) {
            throw new BusinessException("Le deviceId transmis ne correspond pas à l'appareil authentifié", HttpStatus.FORBIDDEN);
        }
        Device device = getDevice(authenticatedDeviceId);
        assertUsageAutorise(device);
        Instant now = Instant.now();
        int inserted = 0;
        int duplicates = 0;
        var errors = new ArrayList<String>();

        for (PresenceItemRequest item : request.getPresences()) {
            if (!authenticatedDeviceId.equals(item.getDeviceId())) {
                errors.add("Événement associé à un autre appareil");
                continue;
            }
            try {
                // La resolution precede le controle d'idempotence : celui-ci porte sur
                // l'identifiant interne, que seul le serveur connait.
                Etudiant etudiant = resoudreParBiometricId(item.getBiometricId());

                if (presenceRepository
                        .existsByDeviceIdAndEtudiantIdAndDatePresenceAndHeurePresence(
                                authenticatedDeviceId, etudiant.getId(), item.getDate(),
                                item.getHeure())) {
                    duplicates++;
                    continue;
                }
                Presence presence = new Presence();
                presence.setEtudiant(etudiant);
                presence.setDevice(device);
                presence.setDatePresence(item.getDate());
                presence.setHeurePresence(item.getHeure());
                presence.setStatut(item.getStatut());
                presence.setSource(SourcePresence.ESP32);
                presence.setCreeLeDevice(item.getCreatedAt());
                presence.setSynchroniseLe(now);
                presence.setSeance(seanceDuReleve(etudiant, device, item));
                presenceRepository.save(presence);
                inserted++;
            } catch (ResourceNotFoundException | BusinessException ex) {
                errors.add(ex.getMessage());
            }
        }

        device.setDerniereConnexion(now);
        device.setDerniereSynchronisation(now);
        deviceRepository.save(device);
        HistoriqueSynchronisation historique = new HistoriqueSynchronisation();
        historique.setDevice(device);
        historique.setDateHeure(now);
        historique.setNbEvenements(request.getPresences().size());
        historique.setNombreTentatives(request.getNombreTentatives());
        historique.setStatut(errors.isEmpty() ? StatutSynchronisation.SUCCES
                : (inserted == 0 && duplicates == 0 ? StatutSynchronisation.ECHEC : StatutSynchronisation.PARTIEL));
        historique.setMessageErreur(errors.isEmpty() ? null : String.join("; ", errors));
        historique = historiqueRepository.save(historique);

        return SyncSummaryResponse.builder().syncId(historique.getId()).deviceId(device.getId())
                .totalTraites(request.getPresences().size()).totalInseres(inserted)
                .totalIgnoresDoublons(duplicates).synchronizedAt(now).build();
    }

    /**
     * Tolérance d'arrivée avant le début du cours.
     *
     * <p>Un étudiant badge en entrant, donc quelques minutes avant l'heure. Sans cette
     * marge, tous les relevés d'arrivée resteraient orphelins.</p>
     */
    private static final Duration AVANCE_TOLEREE = Duration.ofMinutes(30);

    /**
     * Retrouve la séance à laquelle rattacher un relevé.
     *
     * <p>Le lecteur ne transmet qu'un étudiant et un horodatage : il ignore tout de
     * l'emploi du temps. Sans cette résolution côté serveur, chaque relevé restait
     * orphelin — la feuille de présence de l'enseignant affichait alors toute la classe
     * absente alors même que le capteur avait identifié les étudiants.</p>
     *
     * <p>Quand plusieurs séances se chevauchent, celle qui se tient dans la salle du
     * lecteur l'emporte. À défaut de salle sur l'appareil — cas d'un lecteur d'entrée —
     * la séance la plus proche dans le temps est retenue.</p>
     *
     * @return la séance trouvée, ou {@code null} si le relevé ne tombe dans aucune ;
     *         un passage hors cours reste un fait, et mérite d'être conservé
     */
    private Seance seanceDuReleve(Etudiant etudiant, Device device, PresenceItemRequest item) {
        if (etudiant.getClasse() == null) {
            return null;
        }

        Instant instant = item.getDate().atTime(item.getHeure())
                .atZone(ZoneId.systemDefault()).toInstant();

        List<Seance> candidates = seanceRepository.candidatesPourReleve(
                etudiant.getClasse().getId(), instant, instant.plus(AVANCE_TOLEREE));
        if (candidates.isEmpty()) {
            return null;
        }

        Long salleDuLecteur = device.getSalle() == null ? null : device.getSalle().getId();
        if (salleDuLecteur != null) {
            Optional<Seance> memeSalle = candidates.stream()
                    .filter(s -> s.getSalle() != null
                            && s.getSalle().getId().equals(salleDuLecteur))
                    .findFirst();
            if (memeSalle.isPresent()) {
                return memeSalle.get();
            }
        }

        return candidates.get(0);
    }

    /**
     * Vérifie que l'appareil est habilité à relever des présences d'étudiants.
     *
     * <p>Symétrique du contrôle appliqué au pointage du personnel : un lecteur d'entrée
     * dédié aux agents ne doit pas alimenter le flux académique.</p>
     */
    private void assertUsageAutorise(Device device) {
        UsageDevice usage = device.getUsage() == null ? UsageDevice.MIXTE : device.getUsage();
        if (usage == UsageDevice.PERSONNEL) {
            throw new BusinessException(
                    "L'appareil « " + device.getNom() + " » est un lecteur d'entrée dédié au personnel : "
                            + "il n'est pas habilité aux présences des étudiants", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Retrouve l'etudiant a partir de la reference logique transmise par le lecteur.
     *
     * <p>Une reference inconnue signale un enrolement absent ou revoque : l'evenement
     * est rejete et compte parmi les erreurs du lot, sans interrompre les autres.</p>
     */
    private Etudiant resoudreParBiometricId(String biometricId) {
        Etudiant etudiant = etudiantRepository.findByBiometricId(biometricId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Étudiant", "biometricId", biometricId));
        if (!etudiant.isActif()) {
            throw new BusinessException(
                    "L'étudiant " + etudiant.getMatricule() + " est inactif");
        }
        return etudiant;
    }

    private Etudiant getActiveStudent(UUID id) {
        Etudiant etudiant = etudiantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant", "id", id));
        if (!etudiant.isActif()) {
            throw new BusinessException("L'étudiant est inactif", HttpStatus.CONFLICT);
        }
        return etudiant;
    }

    private Device getDevice(UUID id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appareil", "id", id));
    }
}
