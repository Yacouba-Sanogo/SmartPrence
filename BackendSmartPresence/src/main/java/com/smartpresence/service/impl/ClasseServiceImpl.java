package com.smartpresence.service.impl;

import com.smartpresence.dto.request.ClasseRequest;
import com.smartpresence.dto.response.ClasseDetailResponse;
import com.smartpresence.dto.response.ClasseResponse;
import com.smartpresence.entity.Classe;
import com.smartpresence.entity.Personnel;
import com.smartpresence.entity.Filiere;
import com.smartpresence.entity.OptionFiliere;
import com.smartpresence.repository.OptionFiliereRepository;
import com.smartpresence.entity.Promotion;
import com.smartpresence.repository.FiliereRepository;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.ClasseMapper;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.constants.TypePersonnel;
import com.smartpresence.repository.PersonnelRepository;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.PromotionRepository;
import com.smartpresence.service.ClasseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClasseServiceImpl implements ClasseService {

    private final ClasseRepository classeRepository;
    private final PromotionRepository promotionRepository;
    private final FiliereRepository filiereRepository;
    private final OptionFiliereRepository optionFiliereRepository;
    private final PersonnelRepository personnelRepository;
    private final EtudiantRepository etudiantRepository;
    private final ClasseMapper classeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ClasseResponse> findAll() {
        return classeMapper.toResponseList(classeRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public ClasseDetailResponse findById(Long id) {
        return classeMapper.toDetailResponse(getClasse(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClasseResponse> findByPromotionId(Long promotionId) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new ResourceNotFoundException("Promotion", "id", promotionId);
        }
        return classeMapper.toResponseList(classeRepository.findByPromotionId(promotionId));
    }

    @Override
    @Transactional
    public ClasseResponse create(ClasseRequest request) {
        if (classeRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Une classe existe déjà avec le code : " + request.getCode());
        }
        Classe classe = classeMapper.toEntity(request);
        classe.setPromotion(getPromotion(request.getPromotionId()));
        classe.setFiliere(getFiliere(request.getFiliereId()));
        appliquerParcours(classe, request);
        classe.setEnseignants(resolveEnseignants(request.getEnseignantIds()));
        return classeMapper.toResponse(classeRepository.save(classe));
    }

    @Override
    @Transactional
    public ClasseResponse update(Long id, ClasseRequest request) {
        Classe classe = getClasse(id);
        if (!classe.getCode().equals(request.getCode()) && classeRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Une classe existe déjà avec le code : " + request.getCode());
        }
        classeMapper.updateEntityFromRequest(request, classe);
        classe.setPromotion(getPromotion(request.getPromotionId()));
        // Absente de la requête, la filière est conservée : les clients antérieurs au
        // référentiel (le web actuel) ne la connaissent pas et la détacheraient sinon.
        if (request.getFiliereId() != null) {
            classe.setFiliere(getFiliere(request.getFiliereId()));
        }
        appliquerParcours(classe, request);
        if (request.getEnseignantIds() != null) {
            classe.setEnseignants(resolveEnseignants(request.getEnseignantIds()));
        }
        return classeMapper.toResponse(classeRepository.save(classe));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Classe classe = getClasse(id);
        if (etudiantRepository.countByClasseId(id) > 0) {
            throw new BusinessException("Impossible de supprimer une classe contenant des étudiants", HttpStatus.CONFLICT);
        }
        if (!classeRepository.findByClassePrecedenteId(id).isEmpty()) {
            throw new BusinessException(
                    "Cette classe est la classe précédente d'une autre : retirez d'abord ce lien",
                    HttpStatus.CONFLICT);
        }
        classeRepository.delete(classe);
    }

    private Classe getClasse(Long id) {
        return classeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Classe", "id", id));
    }

    /**
     * Option et classe précédente, toutes deux facultatives et conservées quand la
     * requête les omet.
     *
     * <p>L'option doit appartenir à la filière de la classe ; si la classe n'en a pas
     * encore, elle prend celle de l'option. La classe précédente ne peut ni être la
     * classe elle-même ni l'un de ses descendants : le lien formerait une boucle et le
     * passage en année supérieure tournerait indéfiniment.</p>
     */
    private void appliquerParcours(Classe classe, ClasseRequest request) {
        if (request.getOptionId() != null) {
            OptionFiliere option = optionFiliereRepository.findById(request.getOptionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Option", "id", request.getOptionId()));
            if (classe.getFiliere() == null) {
                classe.setFiliere(option.getFiliere());
            } else if (!classe.getFiliere().getId().equals(option.getFiliere().getId())) {
                throw new BusinessException(
                        "L'option « " + option.getLibelle() + " » n'appartient pas à la filière « "
                                + classe.getFiliere().getLibelle() + " »", HttpStatus.CONFLICT);
            }
            classe.setOption(option);
        }

        if (request.getClassePrecedenteId() != null) {
            Classe precedente = getClasse(request.getClassePrecedenteId());
            for (Classe c = precedente; c != null; c = c.getClassePrecedente()) {
                if (classe.getId() != null && classe.getId().equals(c.getId())) {
                    throw new BusinessException(
                            "Une classe ne peut pas être sa propre classe précédente, ni celle d'une "
                                    + "classe dont elle descend", HttpStatus.CONFLICT);
                }
            }
            classe.setClassePrecedente(precedente);
        }
    }

    /** Filière facultative : sans identifiant, la classe n'est rattachée à aucune. */
    private Filiere getFiliere(Long id) {
        if (id == null) {
            return null;
        }
        return filiereRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filière", "id", id));
    }

    private Promotion getPromotion(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", id));
    }

    /**
     * Résout les enseignants d'une classe parmi le personnel.
     *
     * <p>La catégorie est vérifiée explicitement : rien n'empêche techniquement de
     * transmettre l'identifiant d'un agent d'entretien, et le refuser ici vaut mieux
     * que de le découvrir dans un emploi du temps.</p>
     */
    private Set<Personnel> resolveEnseignants(Set<UUID> enseignantIds) {
        if (enseignantIds == null || enseignantIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Personnel> enseignants = new HashSet<>();
        for (UUID enseignantId : enseignantIds) {
            Personnel enseignant = personnelRepository.findById(enseignantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Enseignant", "id", enseignantId));
            if (enseignant.getType() != TypePersonnel.ENSEIGNANT) {
                throw new BusinessException(
                        "L'agent " + enseignant.getMatricule() + " n'est pas de catégorie enseignant",
                        HttpStatus.CONFLICT);
            }
            enseignants.add(enseignant);
        }
        return enseignants;
    }
}
