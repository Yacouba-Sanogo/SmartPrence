package com.smartpresence.service.impl;

import com.smartpresence.constants.RoleCode;
import com.smartpresence.dto.request.InscriptionEtudiantRequest;
import com.smartpresence.dto.response.ClasseResponse;
import com.smartpresence.dto.response.EtudiantResponse;
import com.smartpresence.entity.Classe;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.entity.NumeroCenou;
import com.smartpresence.entity.Role;
import com.smartpresence.entity.Utilisateur;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.EtudiantMapper;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.NumeroCenouRepository;
import com.smartpresence.repository.RoleRepository;
import com.smartpresence.repository.UtilisateurRepository;
import com.smartpresence.service.ClasseService;
import com.smartpresence.service.InscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Inscription libre, adossée au référentiel CENOU.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InscriptionServiceImpl implements InscriptionService {

    private final NumeroCenouRepository numeroCenouRepository;
    private final EtudiantRepository etudiantRepository;
    private final ClasseRepository classeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EtudiantMapper etudiantMapper;
    private final ClasseService classeService;

    /** Domaine servant à fabriquer une adresse de connexion pour un étudiant sans email. */
    @Value("${app.security.domaine-etudiants:etudiant.smartpresence.local}")
    private String domaineEtudiants;

    @Override
    @Transactional(readOnly = true)
    public List<ClasseResponse> classesOuvertes() {
        return classeService.findAll();
    }

    @Override
    @Transactional
    public EtudiantResponse inscrire(InscriptionEtudiantRequest request) {
        String numero = NumeroCenouServiceImpl.normaliser(request.getNumeroCenou());

        NumeroCenou reference = numeroCenouRepository.findByNumero(numero)
                .orElseThrow(() -> new BusinessException(
                        "Ce numéro CENOU ne figure pas dans la liste des inscriptions "
                                + "autorisées. Rapprochez-vous de la scolarité.",
                        HttpStatus.FORBIDDEN));

        if (reference.isUtilise()) {
            throw new BusinessException(
                    "Ce numéro CENOU a déjà servi à une inscription", HttpStatus.CONFLICT);
        }
        // Ceinture et bretelles : le référentiel pourrait avoir été vidé puis
        // réalimenté alors que l'étudiant existe toujours.
        if (etudiantRepository.existsByMatricule(numero)) {
            throw new BusinessException(
                    "Un étudiant est déjà enregistré sous ce matricule", HttpStatus.CONFLICT);
        }

        Classe classe = classeRetenue(reference, request.getClasseId());
        String identifiant = identifiantDeConnexion(request.getEmail(), numero);

        if (utilisateurRepository.existsByEmail(identifiant)) {
            throw new BusinessException(
                    "Un compte utilise déjà l'adresse " + identifiant, HttpStatus.CONFLICT);
        }

        Role roleEtudiant = roleRepository.findByCode(RoleCode.ETUDIANT.name())
                .orElseThrow(() -> new ResourceNotFoundException("Rôle", "code", RoleCode.ETUDIANT));

        Utilisateur compte = new Utilisateur();
        compte.setEmail(identifiant);
        compte.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        compte.setNom(request.getNom());
        compte.setPrenom(request.getPrenom());
        compte.setActif(true);
        compte.setRoles(Set.of(roleEtudiant));
        compte = utilisateurRepository.save(compte);

        Etudiant etudiant = new Etudiant();
        etudiant.setMatricule(numero);
        etudiant.setNom(request.getNom());
        etudiant.setPrenom(request.getPrenom());
        etudiant.setEmail(request.getEmail());
        etudiant.setTelephone(request.getTelephone());
        etudiant.setDateNaissance(request.getDateNaissance());
        etudiant.setClasse(classe);
        etudiant.setUtilisateur(compte);
        etudiant.setActif(true);
        // L'empreinte n'est pas demandée ici : l'enrôlement est un acte distinct,
        // accompli devant le lecteur, une fois le compte créé.
        etudiant = etudiantRepository.save(etudiant);

        reference.setUtilise(true);
        reference.setDateUtilisation(Instant.now());
        numeroCenouRepository.save(reference);

        log.info("Inscription libre : {} {} — CENOU {} — classe {}",
                etudiant.getPrenom(), etudiant.getNom(), numero, classe.getCode());

        return etudiantMapper.toResponse(etudiant);
    }

    /**
     * Départage la classe du référentiel et celle demandée par le candidat.
     *
     * <p>Lorsque l'administration a prévu la classe en important la liste, elle
     * l'emporte : l'affectation ne se négocie pas au moment de l'inscription. Sinon,
     * le choix du candidat fait foi — et il devient obligatoire, un étudiant ne
     * pouvant exister sans classe.</p>
     */
    private Classe classeRetenue(NumeroCenou reference, Long classeDemandee) {
        if (reference.getClasse() != null) {
            return reference.getClasse();
        }
        if (classeDemandee == null) {
            throw new BusinessException(
                    "Choisissez votre classe pour terminer l'inscription", HttpStatus.BAD_REQUEST);
        }
        return classeRepository.findById(classeDemandee)
                .orElseThrow(() -> new ResourceNotFoundException("Classe", "id", classeDemandee));
    }

    /**
     * Identifiant de connexion : l'adresse personnelle si elle existe, sinon une
     * adresse fabriquée à partir du numéro CENOU.
     *
     * <p>Même règle que pour un compte ouvert par l'administration — un étudiant
     * dépourvu d'adresse électronique doit pouvoir se connecter.</p>
     */
    private String identifiantDeConnexion(String email, String numero) {
        if (email != null && !email.isBlank()) {
            return email.trim().toLowerCase();
        }
        return numero.toLowerCase() + "@" + domaineEtudiants;
    }
}
