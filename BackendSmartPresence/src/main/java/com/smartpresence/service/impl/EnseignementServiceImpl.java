package com.smartpresence.service.impl;

import com.smartpresence.constants.StatutPresence;
import com.smartpresence.constants.StatutSeance;
import com.smartpresence.dto.request.EnseignementRequests;
import com.smartpresence.dto.response.EnseignementResponses;
import com.smartpresence.dto.response.JourneePersonnelResponse;
import com.smartpresence.dto.response.SeanceResponse;
import com.smartpresence.entity.CahierTexte;
import com.smartpresence.entity.Chapitre;
import com.smartpresence.entity.Classe;
import com.smartpresence.entity.Matiere;
import com.smartpresence.entity.Personnel;
import com.smartpresence.entity.Seance;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.repository.CahierTexteRepository;
import com.smartpresence.repository.ChapitreRepository;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.MatiereRepository;
import com.smartpresence.repository.SeanceRepository;
import com.smartpresence.service.EnseignementService;
import com.smartpresence.service.PointagePersonnelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnseignementServiceImpl implements EnseignementService {

    private final EnseignantDuCompte enseignants;
    private final SeanceRepository seanceRepository;
    private final CahierTexteRepository cahierRepository;
    private final ChapitreRepository chapitreRepository;
    private final ClasseRepository classeRepository;
    private final MatiereRepository matiereRepository;
    private final PointagePersonnelService pointageService;

    // ------------------------------------------------------------------
    // Pointage et séances effectuées
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<JourneePersonnelResponse> mesPointages(UUID utilisateurId, LocalDate debut, LocalDate fin) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        return pointageService.historique(enseignant.getId(), debut, fin);
    }

    @Override
    @Transactional
    public SeanceResponse declarerSeance(UUID utilisateurId, UUID seanceId) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        Seance seance = seanceDe(enseignant, seanceId);

        if (seance.getStatut() == StatutSeance.ANNULEE) {
            throw new BusinessException(
                    "Cette séance est annulée : elle ne peut pas être déclarée effectuée",
                    HttpStatus.CONFLICT);
        }
        if (seance.getDebut().isAfter(Instant.now())) {
            throw new BusinessException(
                    "Cette séance n'a pas encore commencé : déclarez-la une fois tenue",
                    HttpStatus.CONFLICT);
        }
        if (seance.getDeclareeLe() == null) {
            // Tronqué à la précision de la base : sinon la première réponse porte des
            // nanosecondes que le second appel, relu en base, n'aurait plus.
            seance.setDeclareeLe(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
            seance.setStatut(StatutSeance.TERMINEE);
            seanceRepository.save(seance);
            log.info("Séance déclarée effectuée — id={}, enseignant={}", seanceId, enseignant.getMatricule());
        }
        return versSeance(seance);
    }

    @Override
    @Transactional(readOnly = true)
    public EnseignementResponses.Bilan bilan(UUID utilisateurId, LocalDate debut, LocalDate fin) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        ZoneId zone = ZoneId.systemDefault();
        Instant de = debut.atStartOfDay(zone).toInstant();
        Instant a = fin.plusDays(1).atStartOfDay(zone).toInstant();
        Instant maintenant = Instant.now();

        List<Seance> seances = seanceRepository.findParEnseignant(enseignant.getId(), de, a);
        int effectuees = 0;
        int annulees = 0;
        int aDeclarer = 0;
        long minutes = 0;
        Map<Long, long[]> parMatiere = new LinkedHashMap<>();
        Map<Long, String> libelles = new LinkedHashMap<>();

        for (Seance s : seances) {
            if (s.getStatut() == StatutSeance.ANNULEE) {
                annulees++;
            } else if (s.getDeclareeLe() != null) {
                effectuees++;
                long duree = Duration.between(s.getDebut(), s.getFin()).toMinutes();
                minutes += duree;
                long[] cumul = parMatiere.computeIfAbsent(s.getMatiere().getId(), k -> new long[2]);
                cumul[0]++;
                cumul[1] += duree;
                libelles.put(s.getMatiere().getId(), s.getMatiere().getLibelle());
            } else if (s.getFin().isBefore(maintenant)) {
                aDeclarer++;
            }
        }

        List<JourneePersonnelResponse> journees = pointageService.historique(enseignant.getId(), debut, fin);
        int enRetard = (int) journees.stream().filter(j -> j.getStatut() == StatutPresence.RETARD).count();

        List<EnseignementResponses.LigneMatiere> lignes = parMatiere.entrySet().stream()
                .map(e -> new EnseignementResponses.LigneMatiere(
                        e.getKey(), libelles.get(e.getKey()), (int) e.getValue()[0], e.getValue()[1]))
                .toList();

        return new EnseignementResponses.Bilan(debut, fin, seances.size(), effectuees, annulees,
                aDeclarer, minutes, journees.size(), enRetard, lignes);
    }

    // ------------------------------------------------------------------
    // Cahier de textes
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public EnseignementResponses.PageCahier cahierDeSeance(UUID utilisateurId, UUID seanceId) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        seanceDe(enseignant, seanceId);
        return cahierRepository.findParSeance(seanceId).map(this::versPage).orElse(null);
    }

    @Override
    @Transactional
    public EnseignementResponses.PageCahier ecrireCahier(UUID utilisateurId, UUID seanceId,
                                                        EnseignementRequests.Cahier requete) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        Seance seance = seanceDe(enseignant, seanceId);
        if (seance.getDebut().isAfter(Instant.now())) {
            throw new BusinessException(
                    "Le cahier de textes se remplit une fois la séance commencée", HttpStatus.CONFLICT);
        }

        Chapitre chapitre = null;
        if (requete.chapitreId() != null) {
            chapitre = chapitreRepository.findAvecContexte(requete.chapitreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chapitre", "id", requete.chapitreId()));
            boolean coherent = chapitre.getEnseignant().getId().equals(enseignant.getId())
                    && chapitre.getClasse().getId().equals(seance.getClasse().getId())
                    && chapitre.getMatiere().getId().equals(seance.getMatiere().getId());
            if (!coherent) {
                throw new BusinessException(
                        "Ce chapitre n'appartient pas au programme de cette séance", HttpStatus.CONFLICT);
            }
        }

        CahierTexte page = cahierRepository.findParSeance(seanceId).orElseGet(() -> {
            CahierTexte neuve = new CahierTexte();
            neuve.setSeance(seance);
            return neuve;
        });
        page.setContenu(requete.contenu().trim());
        page.setTravailAFaire(vide(requete.travailAFaire()));
        page.setChapitre(chapitre);

        if (chapitre != null && requete.chapitreTermine() && !chapitre.isTermine()) {
            marquerTermine(chapitre, true);
            chapitreRepository.save(chapitre);
        }
        return versPage(cahierRepository.save(page));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnseignementResponses.PageCahier> cahierDeClasse(UUID utilisateurId, Long classeId, Long matiereId) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        enseignants.exigerClasse(enseignant, classeId);
        return cahierRepository.findParClasse(classeId, enseignant.getId(), matiereId).stream()
                .map(this::versPage)
                .toList();
    }

    // ------------------------------------------------------------------
    // Syllabus
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<EnseignementResponses.Syllabus> syllabus(UUID utilisateurId, Long classeId, Long matiereId) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        enseignants.exigerClasse(enseignant, classeId);
        Map<Long, List<Chapitre>> parMatiere = new LinkedHashMap<>();
        for (Chapitre c : chapitreRepository.findSyllabus(classeId, enseignant.getId(), matiereId)) {
            parMatiere.computeIfAbsent(c.getMatiere().getId(), k -> new java.util.ArrayList<>()).add(c);
        }
        return parMatiere.entrySet().stream().map(e -> {
            List<EnseignementResponses.Chapitre> chapitres = e.getValue().stream().map(this::versChapitre).toList();
            int termines = (int) chapitres.stream().filter(EnseignementResponses.Chapitre::termine).count();
            return new EnseignementResponses.Syllabus(classeId, e.getKey(), chapitres.size(), termines, chapitres);
        }).toList();
    }

    @Override
    @Transactional
    public EnseignementResponses.Chapitre ajouterChapitre(UUID utilisateurId,
                                                         EnseignementRequests.NouveauChapitre requete) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        enseignants.exigerClasse(enseignant, requete.classeId());
        Classe classe = classeRepository.findById(requete.classeId())
                .orElseThrow(() -> new ResourceNotFoundException("Classe", "id", requete.classeId()));
        Matiere matiere = matiereRepository.findById(requete.matiereId())
                .orElseThrow(() -> new ResourceNotFoundException("Matière", "id", requete.matiereId()));

        Chapitre chapitre = new Chapitre();
        chapitre.setClasse(classe);
        chapitre.setMatiere(matiere);
        chapitre.setEnseignant(enseignant);
        chapitre.setOrdre(chapitreRepository.dernierOrdre(classe.getId(), matiere.getId(), enseignant.getId()) + 1);
        chapitre.setTitre(requete.titre().trim());
        chapitre.setObjectifs(vide(requete.objectifs()));
        return versChapitre(chapitreRepository.save(chapitre));
    }

    @Override
    @Transactional
    public EnseignementResponses.Chapitre modifierChapitre(UUID utilisateurId, UUID chapitreId,
                                                          EnseignementRequests.ModificationChapitre requete) {
        Chapitre chapitre = chapitreDe(enseignants.resoudre(utilisateurId), chapitreId);
        chapitre.setTitre(requete.titre().trim());
        chapitre.setObjectifs(vide(requete.objectifs()));
        if (chapitre.isTermine() != requete.termine()) {
            marquerTermine(chapitre, requete.termine());
        }
        return versChapitre(chapitreRepository.save(chapitre));
    }

    @Override
    @Transactional
    public void supprimerChapitre(UUID utilisateurId, UUID chapitreId) {
        chapitreRepository.delete(chapitreDe(enseignants.resoudre(utilisateurId), chapitreId));
    }

    // ------------------------------------------------------------------

    private void marquerTermine(Chapitre chapitre, boolean termine) {
        chapitre.setTermine(termine);
        chapitre.setTermineLe(termine ? LocalDate.now() : null);
    }

    private Chapitre chapitreDe(Personnel enseignant, UUID chapitreId) {
        Chapitre chapitre = chapitreRepository.findAvecContexte(chapitreId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapitre", "id", chapitreId));
        if (!chapitre.getEnseignant().getId().equals(enseignant.getId())) {
            throw new BusinessException("Ce chapitre n'est pas le vôtre", HttpStatus.FORBIDDEN);
        }
        return chapitre;
    }

    private Seance seanceDe(Personnel enseignant, UUID seanceId) {
        Seance seance = seanceRepository.findAvecContexte(seanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Séance", "id", seanceId));
        if (!seance.getEnseignant().getId().equals(enseignant.getId())) {
            throw new BusinessException("Cette séance n'est pas la vôtre", HttpStatus.FORBIDDEN);
        }
        return seance;
    }

    private static String vide(String texte) {
        return texte == null || texte.isBlank() ? null : texte.trim();
    }

    private SeanceResponse versSeance(Seance s) {
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
                .declareeLe(s.getDeclareeLe())
                .build();
    }

    private EnseignementResponses.Chapitre versChapitre(Chapitre c) {
        return new EnseignementResponses.Chapitre(c.getId(), c.getClasse().getId(), c.getMatiere().getId(),
                c.getMatiere().getLibelle(), c.getOrdre(), c.getTitre(), c.getObjectifs(),
                c.isTermine(), c.getTermineLe());
    }

    private EnseignementResponses.PageCahier versPage(CahierTexte p) {
        Seance s = p.getSeance();
        return new EnseignementResponses.PageCahier(p.getId(), s.getId(), s.getDebut(),
                s.getClasse().getId(), s.getClasse().getCode(), s.getMatiere().getId(),
                s.getMatiere().getLibelle(), p.getContenu(), p.getTravailAFaire(),
                p.getChapitre() == null ? null : p.getChapitre().getId(),
                p.getChapitre() == null ? null : p.getChapitre().getTitre());
    }
}
