package com.smartpresence.service.impl;

import com.smartpresence.constants.RoleCode;
import com.smartpresence.constants.TypePersonnel;
import com.smartpresence.dto.request.EnrolementBiometriqueRequest;
import com.smartpresence.dto.request.PersonnelRequest;
import com.smartpresence.dto.response.ComptePersonnelResponse;
import com.smartpresence.dto.response.PersonnelResponse;
import com.smartpresence.entity.Personnel;
import com.smartpresence.entity.Role;
import com.smartpresence.entity.Utilisateur;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.PersonnelMapper;
import com.smartpresence.repository.DeviceRepository;
import com.smartpresence.repository.PersonnelRepository;
import com.smartpresence.repository.RoleRepository;
import com.smartpresence.repository.UtilisateurRepository;
import com.smartpresence.service.PersonnelService;
import com.smartpresence.utils.GenerateurMotDePasse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Implémentation du référentiel du personnel et de l'enrôlement biométrique.
 *
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonnelServiceImpl implements PersonnelService {

    private final PersonnelRepository personnelRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DeviceRepository deviceRepository;
    private final PersonnelMapper personnelMapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /** Domaine des adresses fabriquees pour les agents sans email personnel. */
    @Value("${app.security.domaine-agents:agent.smartpresence.local}")
    private String domaineAgents;

    @Override
    @Transactional(readOnly = true)
    public List<PersonnelResponse> findAll(TypePersonnel type, boolean actifsSeul) {
        List<Personnel> agents;
        if (type != null) {
            agents = personnelRepository.findByTypeOrderByNomAscPrenomAsc(type);
            if (actifsSeul) {
                agents = agents.stream().filter(Personnel::isActif).toList();
            }
        } else if (actifsSeul) {
            agents = personnelRepository.findByActifTrueOrderByNomAscPrenomAsc();
        } else {
            agents = personnelRepository.findAll(Sort.by("nom").ascending().and(Sort.by("prenom").ascending()));
        }
        return personnelMapper.toResponseList(agents);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonnelResponse findById(UUID id) {
        return personnelMapper.toResponse(getPersonnel(id));
    }

    @Override
    @Transactional
    public PersonnelResponse create(PersonnelRequest request) {
        if (personnelRepository.existsByMatricule(request.getMatricule())) {
            throw new BusinessException(
                    "Un agent existe déjà avec le matricule : " + request.getMatricule(), HttpStatus.CONFLICT);
        }
        Personnel personnel = new Personnel();
        appliquer(personnel, request);
        return personnelMapper.toResponse(personnelRepository.save(personnel));
    }

    @Override
    @Transactional
    public PersonnelResponse update(UUID id, PersonnelRequest request) {
        Personnel personnel = getPersonnel(id);
        if (!personnel.getMatricule().equals(request.getMatricule())
                && personnelRepository.existsByMatricule(request.getMatricule())) {
            throw new BusinessException(
                    "Un agent existe déjà avec le matricule : " + request.getMatricule(), HttpStatus.CONFLICT);
        }
        appliquer(personnel, request);
        return personnelMapper.toResponse(personnelRepository.save(personnel));
    }

    @Override
    @Transactional
    public PersonnelResponse setActif(UUID id, boolean actif) {
        Personnel personnel = getPersonnel(id);
        personnel.setActif(actif);
        return personnelMapper.toResponse(personnelRepository.save(personnel));
    }

    @Override
    @Transactional
    public PersonnelResponse enroler(UUID id, EnrolementBiometriqueRequest request) {
        Personnel personnel = getPersonnel(id);

        if (!personnel.isActif()) {
            throw new BusinessException("Un agent inactif ne peut pas être enrôlé", HttpStatus.CONFLICT);
        }

        String biometricId = request.getBiometricId().trim();
        personnelRepository.findByBiometricId(biometricId).ifPresent(autre -> {
            if (!autre.getId().equals(id)) {
                throw new BusinessException(
                        "Cette référence biométrique est déjà attribuée à l'agent "
                                + autre.getMatricule(), HttpStatus.CONFLICT);
            }
        });

        if (request.getDeviceId() != null && !deviceRepository.existsById(request.getDeviceId())) {
            throw new ResourceNotFoundException("Appareil", "id", request.getDeviceId());
        }

        personnel.setBiometricId(biometricId);
        Personnel enregistre = personnelRepository.save(personnel);
        log.info("Agent enrôlé — matricule={}, biometricId={}, lecteur={}",
                enregistre.getMatricule(), biometricId, request.getDeviceId());
        return personnelMapper.toResponse(enregistre);
    }

    @Override
    @Transactional
    public PersonnelResponse revoquerEnrolement(UUID id) {
        Personnel personnel = getPersonnel(id);
        if (!personnel.isEnrole()) {
            throw new BusinessException("Cet agent n'est pas enrôlé", HttpStatus.CONFLICT);
        }
        log.info("Enrôlement révoqué — matricule={}", personnel.getMatricule());
        personnel.setBiometricId(null);
        return personnelMapper.toResponse(personnelRepository.save(personnel));
    }

    /**
     * Cree le compte de connexion d'un agent.
     *
     * <p>L'identifiant est l'email de l'agent lorsqu'il en a un, sinon une adresse
     * derivee de son matricule : tous les agents n'ont pas d'email personnel, et le
     * matricule est la seule donnee dont on soit sur.</p>
     */
    @Override
    @Transactional
    public ComptePersonnelResponse ouvrirCompte(UUID id) {
        Personnel personnel = getPersonnel(id);

        if (personnel.getUtilisateur() != null) {
            throw new BusinessException(
                    "Cet agent dispose deja d'un acces", HttpStatus.CONFLICT);
        }
        if (!personnel.isActif()) {
            throw new BusinessException(
                    "Un agent inactif ne peut pas recevoir d'acces", HttpStatus.CONFLICT);
        }

        String email = identifiantDeConnexion(personnel);
        if (utilisateurRepository.existsByEmail(email)) {
            throw new BusinessException(
                    "Un compte utilise deja l'adresse " + email, HttpStatus.CONFLICT);
        }

        // La categorie decide du role : un enseignant a besoin de ses classes et de ses
        // seances, un agent administratif seulement de ses propres pointages.
        RoleCode roleCode = personnel.getType() == TypePersonnel.ENSEIGNANT
                ? RoleCode.ENSEIGNANT
                : RoleCode.PERSONNEL;
        Role role = roleRepository.findByCode(roleCode.name())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "code", roleCode));

        String motDePasseClair = GenerateurMotDePasse.generer();

        Utilisateur compte = new Utilisateur();
        compte.setEmail(email);
        compte.setMotDePasse(passwordEncoder.encode(motDePasseClair));
        compte.setNom(personnel.getNom());
        compte.setPrenom(personnel.getPrenom());
        compte.setActif(true);
        compte.setRoles(Set.of(role));
        compte = utilisateurRepository.save(compte);

        personnel.setUtilisateur(compte);
        personnelRepository.save(personnel);

        log.info("Acces ouvert pour l'agent {} ({}) avec le role {}",
                personnel.getMatricule(), email, roleCode);

        return ComptePersonnelResponse.builder()
                .personnelId(personnel.getId())
                .matricule(personnel.getMatricule())
                .nomComplet(personnel.getPrenom() + " " + personnel.getNom())
                .type(personnel.getType())
                .utilisateurId(compte.getId())
                .email(email)
                .motDePasseInitial(motDePasseClair)
                .role(roleCode.name())
                .build();
    }

    @Override
    @Transactional
    public PersonnelResponse fermerCompte(UUID id) {
        Personnel personnel = getPersonnel(id);
        Utilisateur compte = personnel.getUtilisateur();
        if (compte == null) {
            throw new BusinessException("Cet agent n'a pas d'acces", HttpStatus.CONFLICT);
        }

        // Le lien est retire avant la suppression : la cle etrangere vit cote personnel.
        personnel.setUtilisateur(null);
        personnelRepository.save(personnel);
        utilisateurRepository.delete(compte);

        log.info("Acces ferme pour l'agent {}", personnel.getMatricule());
        return personnelMapper.toResponse(personnel);
    }

    /** Email de l'agent s'il en a un, sinon une adresse batie sur son matricule. */
    private String identifiantDeConnexion(Personnel personnel) {
        String email = personnel.getEmail();
        if (email != null && !email.isBlank()) {
            return email.trim().toLowerCase();
        }
        return personnel.getMatricule().trim().toLowerCase() + "@" + domaineAgents;
    }

    /**
     * Reporte les champs administratifs de la requête sur l'entité.
     *
     * <p>{@code biometricId} n'est volontairement <b>pas</b> modifiable ici : l'enrôlement
     * est un acte métier distinct, tracé par son propre endpoint.</p>
     */
    private void appliquer(Personnel personnel, PersonnelRequest request) {
        personnel.setMatricule(request.getMatricule());
        personnel.setNom(request.getNom());
        personnel.setPrenom(request.getPrenom());
        personnel.setEmail(request.getEmail());
        personnel.setTelephone(request.getTelephone());
        personnel.setType(request.getType());
        personnel.setService(request.getService());
        if (request.getActif() != null) {
            personnel.setActif(request.getActif());
        }
        if (request.getUtilisateurId() != null) {
            personnel.setUtilisateur(utilisateurRepository.findById(request.getUtilisateurId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Utilisateur", "id", request.getUtilisateurId())));
        }
    }

    private Personnel getPersonnel(UUID id) {
        return personnelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel", "id", id));
    }
}
