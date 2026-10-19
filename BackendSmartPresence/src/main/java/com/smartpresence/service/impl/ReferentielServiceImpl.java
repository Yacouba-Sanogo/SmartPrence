package com.smartpresence.service.impl;

import com.smartpresence.dto.request.ReferentielRequests;
import com.smartpresence.dto.response.DepartementResponse;
import com.smartpresence.dto.response.FiliereResponse;
import com.smartpresence.entity.Departement;
import com.smartpresence.entity.Filiere;
import com.smartpresence.entity.OptionFiliere;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.repository.DepartementRepository;
import com.smartpresence.repository.FiliereRepository;
import com.smartpresence.repository.OptionFiliereRepository;
import com.smartpresence.service.ReferentielService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReferentielServiceImpl implements ReferentielService {

    private final DepartementRepository departementRepository;
    private final FiliereRepository filiereRepository;
    private final OptionFiliereRepository optionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DepartementResponse> departements() {
        return departementRepository.findAllByOrderByLibelleAsc().stream()
                .map(ReferentielServiceImpl::vers).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FiliereResponse> filieres() {
        return filiereRepository.findAllByOrderByLibelleAsc().stream()
                .map(ReferentielServiceImpl::vers).toList();
    }

    @Override
    @Transactional
    public DepartementResponse creerDepartement(ReferentielRequests.Departement requete) {
        String code = requete.code().trim().toUpperCase();
        if (departementRepository.existsByCode(code)) {
            throw dejaUtilise(code);
        }
        Departement d = new Departement();
        d.setCode(code);
        d.setLibelle(requete.libelle().trim());
        return vers(departementRepository.save(d));
    }

    @Override
    @Transactional
    public FiliereResponse creerFiliere(ReferentielRequests.Filiere requete) {
        String code = requete.code().trim().toUpperCase();
        if (filiereRepository.existsByCode(code)) {
            throw dejaUtilise(code);
        }
        Departement departement = departementRepository.findById(requete.departementId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Département", "id", requete.departementId()));
        Filiere f = new Filiere();
        f.setCode(code);
        f.setLibelle(requete.libelle().trim());
        f.setDepartement(departement);
        return vers(filiereRepository.save(f));
    }

    @Override
    @Transactional
    public FiliereResponse ajouterOption(ReferentielRequests.Option requete) {
        String code = requete.code().trim().toUpperCase();
        if (optionRepository.existsByCode(code)) {
            throw dejaUtilise(code);
        }
        Filiere filiere = filiereRepository.findById(requete.filiereId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Filière", "id", requete.filiereId()));
        OptionFiliere o = new OptionFiliere();
        o.setCode(code);
        o.setLibelle(requete.libelle().trim());
        o.setFiliere(filiere);
        filiere.getOptions().add(optionRepository.save(o));
        return vers(filiere);
    }

    @Override
    @Transactional
    public void supprimerOption(Long id) {
        if (!optionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Option", "id", id);
        }
        optionRepository.deleteById(id);
    }

    private static BusinessException dejaUtilise(String code) {
        return new BusinessException("Le code « " + code + " » est déjà utilisé", HttpStatus.CONFLICT);
    }

    private static DepartementResponse vers(Departement d) {
        return new DepartementResponse(d.getId(), d.getCode(), d.getLibelle());
    }

    private static FiliereResponse vers(Filiere f) {
        return new FiliereResponse(f.getId(), f.getCode(), f.getLibelle(), vers(f.getDepartement()),
                f.getOptions().stream()
                        .map(o -> new FiliereResponse.OptionResponse(o.getId(), o.getCode(), o.getLibelle()))
                        .toList());
    }
}
