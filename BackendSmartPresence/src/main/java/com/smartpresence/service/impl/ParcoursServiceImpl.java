package com.smartpresence.service.impl;

import com.smartpresence.constants.DecisionSemestre;
import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.dto.response.ChoixOptionResponse;
import com.smartpresence.dto.response.EtudiantResponse;
import com.smartpresence.dto.response.FiliereResponse;
import com.smartpresence.dto.response.PassageResponse;
import com.smartpresence.dto.response.PassageResponse.Issue;
import com.smartpresence.dto.response.ReleveSemestreResponse;
import com.smartpresence.entity.Classe;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.entity.OptionFiliere;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.EtudiantMapper;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.OptionFiliereRepository;
import com.smartpresence.service.ParcoursService;
import com.smartpresence.service.ReleveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParcoursServiceImpl implements ParcoursService {

    private final EtudiantRepository etudiantRepository;
    private final ClasseRepository classeRepository;
    private final OptionFiliereRepository optionRepository;
    private final ReleveService releveService;
    private final EtudiantMapper etudiantMapper;

    @Override
    @Transactional
    public EtudiantResponse changerClasse(UUID etudiantId, Long classeId) {
        Etudiant etudiant = etudiant(etudiantId);
        Classe cible = classeRepository.findById(classeId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe", "id", classeId));
        etudiant.setClasse(cible);
        classeRepository.libererResponsable(etudiant.getId(), cible.getId());
        // Une option choisie pour une autre filière n'a plus de sens dans la nouvelle.
        if (etudiant.getOptionChoisie() != null && !memeFiliere(etudiant.getOptionChoisie(), cible)) {
            etudiant.setOptionChoisie(null);
        }
        return etudiantMapper.toResponse(etudiantRepository.save(etudiant));
    }

    @Override
    @Transactional(readOnly = true)
    public ChoixOptionResponse options(UUID etudiantId) {
        return choix(etudiant(etudiantId));
    }

    @Override
    @Transactional
    public ChoixOptionResponse choisirOption(UUID etudiantId, Long optionId) {
        Etudiant etudiant = etudiant(etudiantId);
        OptionFiliere option = optionRepository.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Option", "id", optionId));
        if (!memeFiliere(option, etudiant.getClasse())) {
            throw new BusinessException(
                    "Cette option n'est pas proposée par la filière de votre classe", HttpStatus.CONFLICT);
        }
        etudiant.setOptionChoisie(option);
        etudiantRepository.save(etudiant);
        return choix(etudiant);
    }

    @Override
    @Transactional
    public PassageResponse passage(Long classeId, boolean appliquer) {
        List<Classe> classes = classeId == null
                ? classeRepository.findAll()
                : List.of(classeRepository.findById(classeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Classe", "id", classeId)));

        // Phase 1 : décider de tout, sans rien déplacer.
        Map<Etudiant, Classe> deplacements = new LinkedHashMap<>();
        List<PassageResponse.Ligne> lignes = new ArrayList<>();
        int[] compte = new int[Issue.values().length];

        for (Classe classe : classes) {
            for (Etudiant etudiant : etudiantRepository.findByClasseId(classe.getId())) {
                if (!etudiant.isActif()) {
                    continue;
                }
                Decision d = decider(etudiant, classe);
                compte[d.issue().ordinal()]++;
                if (d.destination() != null) {
                    deplacements.put(etudiant, d.destination());
                }
                lignes.add(new PassageResponse.Ligne(etudiant.getId(), etudiant.getMatricule(),
                        etudiant.getPrenom() + " " + etudiant.getNom(), classe.getCode(),
                        d.destination() == null ? null : d.destination().getCode(),
                        d.issue(), d.detail()));
            }
        }

        // Phase 2 : appliquer.
        if (appliquer) {
            deplacements.forEach((etudiant, destination) -> {
                etudiant.setClasse(destination);
                classeRepository.libererResponsable(etudiant.getId(), destination.getId());
                // L'option servait à choisir la classe ; la nouvelle classe porte la sienne.
                etudiantRepository.save(etudiant);
            });
            log.info("Passage en année supérieure appliqué : {} étudiant(s) promu(s)", deplacements.size());
        }

        return new PassageResponse(appliquer, compte[Issue.PROMU.ordinal()],
                compte[Issue.REDOUBLANT.ordinal()], compte[Issue.EN_ATTENTE.ordinal()],
                compte[Issue.TERMINE.ordinal()], compte[Issue.OPTION_A_CHOISIR.ordinal()], lignes);
    }

    // ------------------------------------------------------------------

    private record Decision(Issue issue, Classe destination, String detail) {
    }

    private Decision decider(Etudiant etudiant, Classe classe) {
        ReleveSemestreResponse premier = releveService.releveDe(etudiant.getId(), null);
        List<PeriodeScolaire> semestres = premier.getSemestresDisponibles();
        if (semestres == null || semestres.isEmpty()) {
            return new Decision(Issue.EN_ATTENTE, null, "Aucun semestre dans la maquette de la promotion");
        }

        boolean enAttente = false;
        for (PeriodeScolaire semestre : semestres) {
            ReleveSemestreResponse releve = semestre == premier.getSemestre()
                    ? premier : releveService.releveDe(etudiant.getId(), semestre);
            if (releve.getDecision() == DecisionSemestre.NON_VALIDE) {
                return new Decision(Issue.REDOUBLANT, null,
                        "Semestre " + semestre.libelleCourt() + " non validé");
            }
            if (releve.getDecision() != DecisionSemestre.VALIDE) {
                enAttente = true;
            }
        }
        if (enAttente) {
            return new Decision(Issue.EN_ATTENTE, null, "Notes manquantes sur au moins un semestre");
        }

        List<Classe> suivantes = classeRepository.findByClassePrecedenteId(classe.getId());
        if (suivantes.isEmpty()) {
            return new Decision(Issue.TERMINE, null, "Admis, aucune classe supérieure définie");
        }
        if (suivantes.size() == 1) {
            return new Decision(Issue.PROMU, suivantes.get(0), "Admis");
        }

        OptionFiliere voulue = etudiant.getOptionChoisie();
        if (voulue == null) {
            return new Decision(Issue.OPTION_A_CHOISIR, null,
                    "Admis, mais la filière se divise en options et il n'a pas choisi la sienne");
        }
        return suivantes.stream()
                .filter(c -> c.getOption() != null && c.getOption().getId().equals(voulue.getId()))
                .findFirst()
                .map(c -> new Decision(Issue.PROMU, c, "Admis, option " + voulue.getLibelle()))
                .orElseGet(() -> new Decision(Issue.OPTION_A_CHOISIR, null,
                        "Aucune classe supérieure n'existe pour l'option « " + voulue.getLibelle() + " »"));
    }

    private ChoixOptionResponse choix(Etudiant etudiant) {
        var filiere = etudiant.getClasse().getFiliere();
        if (filiere == null) {
            return new ChoixOptionResponse(null, null, List.of());
        }
        List<FiliereResponse.OptionResponse> disponibles = filiere.getOptions().stream()
                .map(o -> new FiliereResponse.OptionResponse(o.getId(), o.getCode(), o.getLibelle()))
                .toList();
        OptionFiliere choisie = etudiant.getOptionChoisie();
        return new ChoixOptionResponse(filiere.getLibelle(),
                choisie == null ? null
                        : new FiliereResponse.OptionResponse(choisie.getId(), choisie.getCode(), choisie.getLibelle()),
                disponibles);
    }

    private static boolean memeFiliere(OptionFiliere option, Classe classe) {
        return classe.getFiliere() != null
                && classe.getFiliere().getId().equals(option.getFiliere().getId());
    }

    private Etudiant etudiant(UUID id) {
        return etudiantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant", "id", id));
    }
}
