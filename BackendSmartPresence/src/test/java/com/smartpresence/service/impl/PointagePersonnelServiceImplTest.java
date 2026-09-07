package com.smartpresence.service.impl;

import com.smartpresence.constants.SensPointage;
import com.smartpresence.constants.SourcePresence;
import com.smartpresence.constants.StatutPresence;
import com.smartpresence.constants.TypePersonnel;
import com.smartpresence.constants.UsageDevice;
import com.smartpresence.dto.request.PointageESP32Request;
import com.smartpresence.dto.request.PointageItemRequest;
import com.smartpresence.dto.response.JourneePersonnelResponse;
import com.smartpresence.dto.response.SyncSummaryResponse;
import com.smartpresence.entity.Device;
import com.smartpresence.entity.HistoriqueSynchronisation;
import com.smartpresence.entity.ParametreEtablissement;
import com.smartpresence.entity.Personnel;
import com.smartpresence.entity.PresencePersonnel;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.mapper.PresencePersonnelMapper;
import com.smartpresence.repository.DeviceRepository;
import com.smartpresence.repository.HistoriqueSynchronisationRepository;
import com.smartpresence.repository.ParametreEtablissementRepository;
import com.smartpresence.repository.PersonnelRepository;
import com.smartpresence.repository.PresencePersonnelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de la logique de pointage du personnel.
 *
 * <p>Couvre les règles qui ne relèvent pas de la persistance : détermination du sens,
 * qualification du retard, idempotence, habilitation de l'appareil et synthèse
 * journalière.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PointagePersonnelServiceImplTest {

    private static final UUID DEVICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PERSONNEL_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String BIOMETRIC_ID = "PER-0042";
    private static final LocalDate JOUR = LocalDate.of(2026, 3, 12);

    @Mock private PresencePersonnelRepository pointageRepository;
    @Mock private PersonnelRepository personnelRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private HistoriqueSynchronisationRepository historiqueRepository;
    @Mock private ParametreEtablissementRepository parametreRepository;
    @Mock private PresencePersonnelMapper pointageMapper;

    @InjectMocks private PointagePersonnelServiceImpl service;

    private Device device;
    private Personnel personnel;

    @BeforeEach
    void setUp() {
        device = new Device();
        device.setId(DEVICE_ID);
        device.setNom("ESP32-Entree-Principale");
        device.setUsage(UsageDevice.PERSONNEL);

        personnel = new Personnel();
        personnel.setId(PERSONNEL_ID);
        personnel.setMatricule("AG-001");
        personnel.setNom("Traoré");
        personnel.setPrenom("Aminata");
        personnel.setType(TypePersonnel.ADMINISTRATIF);
        personnel.setService("Scolarité");
        personnel.setBiometricId(BIOMETRIC_ID);
        personnel.setActif(true);

        ParametreEtablissement parametres = new ParametreEtablissement();
        parametres.setHeureOuverture(LocalTime.of(8, 0));
        parametres.setHeureFermeture(LocalTime.of(17, 0));
        parametres.setSeuilRetardMinutes(15);

        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));
        when(personnelRepository.findByBiometricId(BIOMETRIC_ID)).thenReturn(Optional.of(personnel));
        when(parametreRepository.findById(1L)).thenReturn(Optional.of(parametres));
        when(pointageRepository.save(any(PresencePersonnel.class))).thenAnswer(inv -> inv.getArgument(0));
        when(historiqueRepository.save(any(HistoriqueSynchronisation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Ingestion des pointages ESP32")
    class Ingestion {

        @Test
        @DisplayName("le premier pointage de la journée est enregistré comme une ENTRÉE à l'heure")
        void premierPointageEstUneEntree() {
            when(pointageRepository.findFirstByPersonnelIdAndDatePointageOrderByHeurePointageDesc(
                    PERSONNEL_ID, JOUR)).thenReturn(Optional.empty());

            SyncSummaryResponse resume = service.synchronize(DEVICE_ID, lot(LocalTime.of(7, 52)));

            PresencePersonnel enregistre = capturerPointage();
            assertThat(enregistre.getSens()).isEqualTo(SensPointage.ENTREE);
            assertThat(enregistre.getStatut()).isEqualTo(StatutPresence.PRESENT);
            assertThat(enregistre.getHeurePointage()).isEqualTo(LocalTime.of(7, 52));
            assertThat(enregistre.getSource()).isEqualTo(SourcePresence.ESP32);
            assertThat(resume.getTotalInseres()).isEqualTo(1);
        }

        @Test
        @DisplayName("une arrivée au-delà de la tolérance est qualifiée de RETARD")
        void arriveeTardiveEstUnRetard() {
            when(pointageRepository.findFirstByPersonnelIdAndDatePointageOrderByHeurePointageDesc(
                    PERSONNEL_ID, JOUR)).thenReturn(Optional.empty());

            service.synchronize(DEVICE_ID, lot(LocalTime.of(8, 31)));

            assertThat(capturerPointage().getStatut()).isEqualTo(StatutPresence.RETARD);
        }

        @Test
        @DisplayName("une arrivée dans la tolérance reste PRESENT")
        void arriveeDansLaToleranceResteAlHeure() {
            when(pointageRepository.findFirstByPersonnelIdAndDatePointageOrderByHeurePointageDesc(
                    PERSONNEL_ID, JOUR)).thenReturn(Optional.empty());

            service.synchronize(DEVICE_ID, lot(LocalTime.of(8, 15)));

            assertThat(capturerPointage().getStatut()).isEqualTo(StatutPresence.PRESENT);
        }

        @Test
        @DisplayName("le pointage suivant une entrée est enregistré comme une SORTIE")
        void pointageSuivantUneEntreeEstUneSortie() {
            PresencePersonnel entree = new PresencePersonnel();
            entree.setSens(SensPointage.ENTREE);
            when(pointageRepository.findFirstByPersonnelIdAndDatePointageOrderByHeurePointageDesc(
                    PERSONNEL_ID, JOUR)).thenReturn(Optional.of(entree));

            service.synchronize(DEVICE_ID, lot(LocalTime.of(17, 5)));

            PresencePersonnel enregistre = capturerPointage();
            assertThat(enregistre.getSens()).isEqualTo(SensPointage.SORTIE);
            assertThat(enregistre.getStatut())
                    .as("une sortie tardive n'est jamais un retard")
                    .isEqualTo(StatutPresence.PRESENT);
        }

        @Test
        @DisplayName("un pointage déjà enregistré est compté comme doublon sans réinsertion")
        void pointageDejaConnuEstIgnore() {
            when(pointageRepository.existsByDeviceIdAndPersonnelIdAndDatePointageAndHeurePointage(
                    eq(DEVICE_ID), eq(PERSONNEL_ID), eq(JOUR), any(LocalTime.class))).thenReturn(true);

            SyncSummaryResponse resume = service.synchronize(DEVICE_ID, lot(LocalTime.of(8, 2)));

            assertThat(resume.getTotalIgnoresDoublons()).isEqualTo(1);
            assertThat(resume.getTotalInseres()).isZero();
            verify(pointageRepository, never()).save(any(PresencePersonnel.class));
        }

        @Test
        @DisplayName("un lot arrivé dans le désordre est traité chronologiquement")
        void lotDesordonneEstReordonne() {
            when(pointageRepository.findFirstByPersonnelIdAndDatePointageOrderByHeurePointageDesc(
                    PERSONNEL_ID, JOUR)).thenReturn(Optional.empty());

            PointageESP32Request request = PointageESP32Request.builder()
                    .deviceId(DEVICE_ID)
                    .pointages(List.of(item(LocalTime.of(17, 30)), item(LocalTime.of(7, 55))))
                    .build();

            service.synchronize(DEVICE_ID, request);

            ArgumentCaptor<PresencePersonnel> captor = ArgumentCaptor.forClass(PresencePersonnel.class);
            verify(pointageRepository, times(2)).save(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(PresencePersonnel::getHeurePointage)
                    .containsExactly(LocalTime.of(7, 55), LocalTime.of(17, 30));
        }

        @Test
        @DisplayName("un agent inconnu du référentiel n'interrompt pas le lot")
        void agentInconnuEstSignaleSansBloquer() {
            when(personnelRepository.findByBiometricId(anyString())).thenReturn(Optional.empty());

            SyncSummaryResponse resume = service.synchronize(DEVICE_ID, lot(LocalTime.of(8, 0)));

            assertThat(resume.getTotalInseres()).isZero();
            assertThat(resume.getTotalTraites()).isEqualTo(1);
            verify(historiqueRepository).save(any(HistoriqueSynchronisation.class));
        }

        @Test
        @DisplayName("un lecteur de salle est refusé sur le flux du personnel")
        void lecteurEtudiantEstRefuse() {
            device.setUsage(UsageDevice.ETUDIANT);

            assertThatThrownBy(() -> service.synchronize(DEVICE_ID, lot(LocalTime.of(8, 0))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("lecteur de salle");
        }

        @Test
        @DisplayName("un deviceId de payload différent de l'appareil authentifié est rejeté")
        void deviceIdIncoherentEstRejete() {
            UUID autre = UUID.randomUUID();
            PointageESP32Request request = PointageESP32Request.builder()
                    .deviceId(autre)
                    .pointages(List.of(item(LocalTime.of(8, 0))))
                    .build();

            assertThatThrownBy(() -> service.synchronize(DEVICE_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ne correspond pas");
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Synthèse de la journée")
    class Synthese {

        @Test
        @DisplayName("entrée et sortie donnent l'heure d'arrivée et le temps de présence")
        void journeeCompleteEstSynthetisee() {
            when(pointageRepository.findJourneeComplete(JOUR)).thenReturn(List.of(
                    pointage(LocalTime.of(8, 5), SensPointage.ENTREE, StatutPresence.PRESENT),
                    pointage(LocalTime.of(17, 5), SensPointage.SORTIE, StatutPresence.PRESENT)));
            when(personnelRepository.findByActifTrueOrderByNomAscPrenomAsc())
                    .thenReturn(List.of(personnel));

            JourneePersonnelResponse journee = service.journee(JOUR).getFirst();

            assertThat(journee.getHeureEntree()).isEqualTo(LocalTime.of(8, 5));
            assertThat(journee.getHeureSortie()).isEqualTo(LocalTime.of(17, 5));
            assertThat(journee.getMinutesTravaillees()).isEqualTo(540);
            assertThat(journee.getMinutesRetard()).isEqualTo(5);
            assertThat(journee.getStatut()).isEqualTo(StatutPresence.PRESENT);
            assertThat(journee.isPresent()).isFalse();
            assertThat(journee.getNombrePointages()).isEqualTo(2);
        }

        @Test
        @DisplayName("un agent entré mais non sorti est signalé comme encore présent")
        void agentSansSortieEstEncorePresent() {
            when(pointageRepository.findJourneeComplete(JOUR)).thenReturn(List.of(
                    pointage(LocalTime.of(7, 58), SensPointage.ENTREE, StatutPresence.PRESENT)));
            when(personnelRepository.findByActifTrueOrderByNomAscPrenomAsc())
                    .thenReturn(List.of(personnel));

            JourneePersonnelResponse journee = service.journee(JOUR).getFirst();

            assertThat(journee.isPresent()).isTrue();
            assertThat(journee.getHeureSortie()).isNull();
            assertThat(journee.getMinutesTravaillees()).isNull();
            assertThat(journee.getMinutesRetard()).isZero();
            assertThat(journee.getMinutesEcoulees())
                    .as("un temps écoulé n'a pas de sens sur une date passée")
                    .isNull();
        }

        @Test
        @DisplayName("le temps écoulé est calculé pour un agent présent dans la journée en cours")
        void tempsEcouleCalculePourLaJourneeEnCours() {
            ZoneId zone = ZoneId.of("Africa/Bamako");
            LocalTime maintenant = LocalTime.now(zone);
            assumeTrue(maintenant.isAfter(LocalTime.of(0, 5)),
                    "non pertinent dans les toutes premières minutes de la journée");

            LocalDate aujourdhui = LocalDate.now(zone);
            LocalTime arrivee = LocalTime.of(0, 1);
            long attendu = Duration.between(arrivee, maintenant).toMinutes();

            PresencePersonnel entree = pointage(arrivee, SensPointage.ENTREE, StatutPresence.PRESENT);
            entree.setDatePointage(aujourdhui);
            when(pointageRepository.findJourneeComplete(aujourdhui)).thenReturn(List.of(entree));
            when(personnelRepository.findByActifTrueOrderByNomAscPrenomAsc())
                    .thenReturn(List.of(personnel));

            JourneePersonnelResponse journee = service.journee(aujourdhui).getFirst();

            assertThat(journee.getMinutesEcoulees()).isNotNull().isBetween(attendu - 1, attendu + 1);
            assertThat(journee.getMinutesTravaillees()).isNull();
            assertThat(journee.isPresent()).isTrue();
        }

        @Test
        @DisplayName("un agent actif sans aucun pointage apparaît en ABSENT")
        void agentSansPointageEstAbsent() {
            when(pointageRepository.findJourneeComplete(JOUR)).thenReturn(List.of());
            when(personnelRepository.findByActifTrueOrderByNomAscPrenomAsc())
                    .thenReturn(List.of(personnel));

            JourneePersonnelResponse journee = service.journee(JOUR).getFirst();

            assertThat(journee.getStatut()).isEqualTo(StatutPresence.ABSENT);
            assertThat(journee.getHeureEntree()).isNull();
            assertThat(journee.getNombrePointages()).isZero();
            assertThat(journee.isPresent()).isFalse();
        }
    }

    // ------------------------------------------------------------------
    // Fabriques de test
    // ------------------------------------------------------------------

    private PointageESP32Request lot(LocalTime heure) {
        return PointageESP32Request.builder()
                .deviceId(DEVICE_ID)
                .pointages(List.of(item(heure)))
                .nombreTentatives(1)
                .build();
    }

    private PointageItemRequest item(LocalTime heure) {
        return PointageItemRequest.builder()
                .biometricId(BIOMETRIC_ID)
                .deviceId(DEVICE_ID)
                .date(JOUR)
                .heure(heure)
                .createdAt(Instant.parse("2026-03-12T07:52:00Z"))
                .build();
    }

    private PresencePersonnel pointage(LocalTime heure, SensPointage sens, StatutPresence statut) {
        PresencePersonnel p = new PresencePersonnel();
        p.setPersonnel(personnel);
        p.setDevice(device);
        p.setDatePointage(JOUR);
        p.setHeurePointage(heure);
        p.setSens(sens);
        p.setStatut(statut);
        return p;
    }

    private PresencePersonnel capturerPointage() {
        ArgumentCaptor<PresencePersonnel> captor = ArgumentCaptor.forClass(PresencePersonnel.class);
        verify(pointageRepository).save(captor.capture());
        return captor.getValue();
    }
}
