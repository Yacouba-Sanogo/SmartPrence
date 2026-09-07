package com.smartpresence.service.impl;

import com.smartpresence.constants.SensPointage;
import com.smartpresence.constants.SourcePresence;
import com.smartpresence.constants.StatutPresence;
import com.smartpresence.constants.StatutSynchronisation;
import com.smartpresence.constants.UsageDevice;
import com.smartpresence.dto.request.PointageESP32Request;
import com.smartpresence.dto.request.PointageItemRequest;
import com.smartpresence.dto.request.PointageManuelRequest;
import com.smartpresence.dto.response.JourneePersonnelResponse;
import com.smartpresence.dto.response.PointagePersonnelResponse;
import com.smartpresence.dto.response.SyncSummaryResponse;
import com.smartpresence.entity.Device;
import com.smartpresence.entity.HistoriqueSynchronisation;
import com.smartpresence.entity.ParametreEtablissement;
import com.smartpresence.entity.Personnel;
import com.smartpresence.entity.PresencePersonnel;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.PresencePersonnelMapper;
import com.smartpresence.repository.DeviceRepository;
import com.smartpresence.repository.HistoriqueSynchronisationRepository;
import com.smartpresence.repository.ParametreEtablissementRepository;
import com.smartpresence.repository.PersonnelRepository;
import com.smartpresence.repository.PresencePersonnelRepository;
import com.smartpresence.service.PointagePersonnelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implémentation du pointage horaire du personnel.
 *
 * <h2>Détermination du sens</h2>
 * <p>Le lecteur ESP32 ne transmet pas le sens du pointage : il ignore ce qui a déjà été
 * enregistré. Le sens est donc déduit ici — premier pointage de la journée =
 * {@link SensPointage#ENTREE}, puis alternance avec le pointage précédent. Le lot reçu est
 * <b>trié chronologiquement</b> avant traitement, car une file vidée après une coupure
 * réseau peut arriver dans le désordre et fausserait l'alternance.</p>
 *
 * <h2>Qualification du retard</h2>
 * <p>Une entrée postérieure à {@code heureOuverture + seuilRetardMinutes} est marquée
 * {@link StatutPresence#RETARD}. Ces deux valeurs proviennent de
 * {@link ParametreEtablissement} et restent administrables sans redéploiement.</p>
 *
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointagePersonnelServiceImpl implements PointagePersonnelService {

    private final PresencePersonnelRepository pointageRepository;
    private final PersonnelRepository personnelRepository;
    private final DeviceRepository deviceRepository;
    private final HistoriqueSynchronisationRepository historiqueRepository;
    private final ParametreEtablissementRepository parametreRepository;
    private final PresencePersonnelMapper pointageMapper;

    // ------------------------------------------------------------------
    // Ingestion ESP32
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public SyncSummaryResponse synchronize(UUID authenticatedDeviceId, PointageESP32Request request) {
        if (!authenticatedDeviceId.equals(request.getDeviceId())) {
            throw new BusinessException(
                    "Le deviceId transmis ne correspond pas à l'appareil authentifié", HttpStatus.FORBIDDEN);
        }

        Device device = getDevice(authenticatedDeviceId);
        assertUsageAutorise(device);

        ParametreEtablissement parametres = getParametres();
        Instant now = Instant.now();
        int inserted = 0;
        int duplicates = 0;
        List<String> errors = new ArrayList<>();

        for (PointageItemRequest item : ordonnerChronologiquement(request.getPointages())) {
            if (!authenticatedDeviceId.equals(item.getDeviceId())) {
                errors.add("Pointage associé à un autre appareil que celui authentifié");
                continue;
            }
            try {
                Personnel personnel = resoudreParBiometricId(item.getBiometricId());

                if (pointageRepository.existsByDeviceIdAndPersonnelIdAndDatePointageAndHeurePointage(
                        authenticatedDeviceId, personnel.getId(), item.getDate(), item.getHeure())) {
                    duplicates++;
                    continue;
                }

                SensPointage sens = determinerSens(personnel.getId(), item.getDate());

                PresencePersonnel pointage = new PresencePersonnel();
                pointage.setPersonnel(personnel);
                pointage.setDevice(device);
                pointage.setDatePointage(item.getDate());
                pointage.setHeurePointage(item.getHeure());
                pointage.setSens(sens);
                pointage.setStatut(qualifier(sens, item.getHeure(), parametres));
                pointage.setSource(SourcePresence.ESP32);
                pointage.setCreeLeDevice(item.getCreatedAt());
                pointage.setSynchroniseLe(now);
                pointageRepository.save(pointage);
                inserted++;
            } catch (ResourceNotFoundException | BusinessException ex) {
                errors.add(ex.getMessage());
            }
        }

        device.setDerniereConnexion(now);
        device.setDerniereSynchronisation(now);
        deviceRepository.save(device);

        HistoriqueSynchronisation historique = enregistrerHistorique(
                device, now, request, inserted, duplicates, errors);

        log.info("Pointages personnel synchronisés — appareil={}, reçus={}, insérés={}, doublons={}, erreurs={}",
                device.getNom(), request.getPointages().size(), inserted, duplicates, errors.size());

        return SyncSummaryResponse.builder()
                .syncId(historique.getId())
                .deviceId(device.getId())
                .totalTraites(request.getPointages().size())
                .totalInseres(inserted)
                .totalIgnoresDoublons(duplicates)
                .synchronizedAt(now)
                .build();
    }

    // ------------------------------------------------------------------
    // Régularisation administrative
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public PointagePersonnelResponse createManuel(PointageManuelRequest request) {
        Personnel personnel = getPersonnelActif(request.getPersonnelId());
        Device device = request.getDeviceId() == null ? null : getDevice(request.getDeviceId());

        if (pointageRepository.existsByDeviceIdAndPersonnelIdAndDatePointageAndHeurePointage(
                request.getDeviceId(), personnel.getId(),
                request.getDatePointage(), request.getHeurePointage())) {
            throw new BusinessException("Ce pointage existe déjà", HttpStatus.CONFLICT);
        }
        if (request.getDatePointage().isAfter(LocalDate.now())) {
            throw new BusinessException("Un pointage ne peut pas être daté dans le futur");
        }

        Instant now = Instant.now();
        PresencePersonnel pointage = new PresencePersonnel();
        pointage.setPersonnel(personnel);
        pointage.setDevice(device);
        pointage.setDatePointage(request.getDatePointage());
        pointage.setHeurePointage(request.getHeurePointage());
        pointage.setSens(request.getSens());
        pointage.setStatut(qualifier(request.getSens(), request.getHeurePointage(), getParametres()));
        pointage.setSource(SourcePresence.MANUEL);
        pointage.setMotif(request.getMotif().trim());
        pointage.setCreeLeDevice(now);
        pointage.setSynchroniseLe(now);

        log.info("Pointage régularisé — agent={}, date={}, heure={}, sens={}",
                personnel.getMatricule(), request.getDatePointage(),
                request.getHeurePointage(), request.getSens());

        return pointageMapper.toResponse(pointageRepository.save(pointage));
    }

    // ------------------------------------------------------------------
    // Restitution
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<JourneePersonnelResponse> journee(LocalDate date) {
        ParametreEtablissement parametres = getParametres();

        Map<UUID, List<PresencePersonnel>> parAgent = pointageRepository.findJourneeComplete(date).stream()
                .collect(Collectors.groupingBy(p -> p.getPersonnel().getId()));

        return personnelRepository.findByActifTrueOrderByNomAscPrenomAsc().stream()
                .map(personnel -> synthetiser(
                        personnel, date, parAgent.getOrDefault(personnel.getId(), List.of()), parametres))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PointagePersonnelResponse> pointagesDuJour(LocalDate date) {
        List<PresencePersonnel> pointages = new ArrayList<>(pointageRepository.findJourneeComplete(date));
        pointages.sort(Comparator.comparing(PresencePersonnel::getHeurePointage).reversed());
        return pointageMapper.toResponseList(pointages);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JourneePersonnelResponse> historique(UUID personnelId, LocalDate debut, LocalDate fin) {
        if (debut.isAfter(fin)) {
            throw new BusinessException("La date de début doit être antérieure ou égale à la date de fin");
        }
        Personnel personnel = getPersonnel(personnelId);
        ParametreEtablissement parametres = getParametres();

        return pointageRepository
                .findByPersonnelIdAndDatePointageBetweenOrderByDatePointageAscHeurePointageAsc(
                        personnelId, debut, fin)
                .stream()
                .collect(Collectors.groupingBy(PresencePersonnel::getDatePointage))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> synthetiser(personnel, entry.getKey(), entry.getValue(), parametres))
                .toList();
    }

    // ------------------------------------------------------------------
    // Règles métier internes
    // ------------------------------------------------------------------

    /**
     * Trie le lot par date puis heure croissantes.
     *
     * <p>Indispensable : la file NVS d'un appareil peut être vidée dans un ordre
     * quelconque, et l'alternance entrée/sortie serait alors inversée.</p>
     */
    private List<PointageItemRequest> ordonnerChronologiquement(List<PointageItemRequest> items) {
        return items.stream()
                .sorted(Comparator.comparing(PointageItemRequest::getDate)
                        .thenComparing(PointageItemRequest::getHeure))
                .toList();
    }

    /**
     * Déduit le sens du pointage à partir du dernier événement enregistré ce jour-là.
     *
     * @return {@link SensPointage#ENTREE} si aucun pointage n'existe encore, sinon
     *         l'inverse du dernier sens relevé
     */
    private SensPointage determinerSens(UUID personnelId, LocalDate date) {
        return pointageRepository
                .findFirstByPersonnelIdAndDatePointageOrderByHeurePointageDesc(personnelId, date)
                .map(dernier -> dernier.getSens() == SensPointage.ENTREE
                        ? SensPointage.SORTIE
                        : SensPointage.ENTREE)
                .orElse(SensPointage.ENTREE);
    }

    /**
     * Qualifie un pointage d'entrée en {@code PRESENT} ou {@code RETARD}.
     * <p>Une sortie n'est jamais qualifiée de retard.</p>
     */
    private StatutPresence qualifier(SensPointage sens, LocalTime heure, ParametreEtablissement parametres) {
        if (sens != SensPointage.ENTREE) {
            return StatutPresence.PRESENT;
        }
        LocalTime limite = parametres.getHeureOuverture().plusMinutes(parametres.getSeuilRetardMinutes());
        return heure.isAfter(limite) ? StatutPresence.RETARD : StatutPresence.PRESENT;
    }

    /**
     * Condense les pointages bruts d'une journée en une ligne de synthèse.
     *
     * @param pointages pointages de l'agent pour la date, éventuellement vide
     */
    private JourneePersonnelResponse synthetiser(Personnel personnel, LocalDate date,
                                                 List<PresencePersonnel> pointages,
                                                 ParametreEtablissement parametres) {
        LocalTime entree = pointages.stream()
                .filter(p -> p.getSens() == SensPointage.ENTREE)
                .map(PresencePersonnel::getHeurePointage)
                .min(LocalTime::compareTo)
                .orElse(null);

        LocalTime sortie = pointages.stream()
                .filter(p -> p.getSens() == SensPointage.SORTIE)
                .map(PresencePersonnel::getHeurePointage)
                .max(LocalTime::compareTo)
                .orElse(null);

        StatutPresence statut = pointages.stream()
                .filter(p -> p.getSens() == SensPointage.ENTREE)
                .min(Comparator.comparing(PresencePersonnel::getHeurePointage))
                .map(PresencePersonnel::getStatut)
                .orElse(StatutPresence.ABSENT);

        long minutesRetard = 0;
        if (entree != null && entree.isAfter(parametres.getHeureOuverture())) {
            minutesRetard = Duration.between(parametres.getHeureOuverture(), entree).toMinutes();
        }

        Long minutesTravaillees = null;
        if (entree != null && sortie != null && sortie.isAfter(entree)) {
            minutesTravaillees = Duration.between(entree, sortie).toMinutes();
        }

        Long minutesEcoulees = null;
        if (entree != null && sortie == null) {
            ZoneId zone = zoneEtablissement(parametres);
            if (date.equals(LocalDate.now(zone))) {
                LocalTime maintenant = LocalTime.now(zone);
                if (maintenant.isAfter(entree)) {
                    minutesEcoulees = Duration.between(entree, maintenant).toMinutes();
                }
            }
        }

        return JourneePersonnelResponse.builder()
                .personnelId(personnel.getId())
                .matricule(personnel.getMatricule())
                .nom(personnel.getNom())
                .prenom(personnel.getPrenom())
                .type(personnel.getType())
                .service(personnel.getService())
                .date(date)
                .heureEntree(entree)
                .heureSortie(sortie)
                .statut(statut)
                .minutesRetard(minutesRetard)
                .minutesTravaillees(minutesTravaillees)
                .minutesEcoulees(minutesEcoulees)
                .nombrePointages(pointages.size())
                .present(entree != null && sortie == null)
                .build();
    }

    /**
     * Vérifie que l'appareil est habilité à produire des pointages de personnel.
     *
     * <p>Un lecteur de salle dédié aux étudiants ne doit pas alimenter ce flux : le refus
     * est explicite plutôt que silencieux.</p>
     */
    private void assertUsageAutorise(Device device) {
        UsageDevice usage = device.getUsage() == null ? UsageDevice.MIXTE : device.getUsage();
        if (usage == UsageDevice.ETUDIANT) {
            throw new BusinessException(
                    "L'appareil « " + device.getNom() + " » est un lecteur de salle : "
                            + "il n'est pas habilité au pointage du personnel", HttpStatus.FORBIDDEN);
        }
    }

    private Personnel resoudreParBiometricId(String biometricId) {
        Personnel personnel = personnelRepository.findByBiometricId(biometricId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Personnel", "biometricId", biometricId));
        if (!personnel.isActif()) {
            throw new BusinessException(
                    "L'agent " + personnel.getMatricule() + " est inactif", HttpStatus.CONFLICT);
        }
        return personnel;
    }

    private HistoriqueSynchronisation enregistrerHistorique(Device device, Instant now,
                                                            PointageESP32Request request,
                                                            int inserted, int duplicates,
                                                            List<String> errors) {
        HistoriqueSynchronisation historique = new HistoriqueSynchronisation();
        historique.setDevice(device);
        historique.setDateHeure(now);
        historique.setNbEvenements(request.getPointages().size());
        historique.setNombreTentatives(request.getNombreTentatives());
        historique.setStatut(errors.isEmpty()
                ? StatutSynchronisation.SUCCES
                : (inserted == 0 && duplicates == 0
                        ? StatutSynchronisation.ECHEC
                        : StatutSynchronisation.PARTIEL));
        historique.setMessageErreur(errors.isEmpty() ? null : String.join("; ", errors));
        return historiqueRepository.save(historique);
    }

    private Personnel getPersonnel(UUID id) {
        return personnelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel", "id", id));
    }

    private Personnel getPersonnelActif(UUID id) {
        Personnel personnel = getPersonnel(id);
        if (!personnel.isActif()) {
            throw new BusinessException("L'agent est inactif", HttpStatus.CONFLICT);
        }
        return personnel;
    }

    private Device getDevice(UUID id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appareil", "id", id));
    }

    /**
     * Fuseau horaire de l'établissement, avec repli sur celui du serveur.
     *
     * <p>Le fuseau est administrable et donc faillible : un identifiant invalide ne doit
     * pas faire échouer la consultation de la journée.</p>
     */
    private ZoneId zoneEtablissement(ParametreEtablissement parametres) {
        String fuseau = parametres.getFuseauHoraire();
        if (fuseau == null || fuseau.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(fuseau);
        } catch (DateTimeException ex) {
            log.warn("Fuseau horaire invalide dans les paramètres : « {} » — repli sur {}",
                    fuseau, ZoneId.systemDefault());
            return ZoneId.systemDefault();
        }
    }

    private ParametreEtablissement getParametres() {
        return parametreRepository.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Paramètres établissement", "id", 1L));
    }
}
