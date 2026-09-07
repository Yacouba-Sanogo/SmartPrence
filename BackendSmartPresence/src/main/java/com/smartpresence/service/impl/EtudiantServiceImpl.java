package com.smartpresence.service.impl;

import com.smartpresence.constants.RoleCode;
import com.smartpresence.dto.request.EtudiantRequest;
import com.smartpresence.dto.response.CompteEtudiantResponse;
import com.smartpresence.dto.response.EtudiantResponse;
import com.smartpresence.entity.Classe;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.entity.Role;
import com.smartpresence.entity.Utilisateur;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.EtudiantMapper;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.RoleRepository;
import com.smartpresence.repository.UtilisateurRepository;
import com.smartpresence.service.EtudiantService;
import com.smartpresence.utils.GenerateurMotDePasse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtudiantServiceImpl implements EtudiantService {
    private final EtudiantRepository etudiantRepository;
    private final ClasseRepository classeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EtudiantMapper etudiantMapper;

    /** Domaine servant à fabriquer une adresse de connexion pour un étudiant sans email. */
    @Value("${app.security.domaine-etudiants:etudiant.smartpresence.local}")
    private String domaineEtudiants;

    @Override @Transactional(readOnly = true)
    public List<EtudiantResponse> findAll(Long classeId) {
        return etudiantMapper.toResponseList(classeId == null ? etudiantRepository.findAll() : etudiantRepository.findByClasseId(classeId));
    }
    @Override @Transactional(readOnly = true)
    public EtudiantResponse findById(UUID id) { return etudiantMapper.toResponse(getEtudiant(id)); }
    @Override @Transactional
    public EtudiantResponse create(EtudiantRequest request) {
        assertIdentifiersAvailable(request, null);
        Etudiant etudiant = etudiantMapper.toEntity(request);
        etudiant.setClasse(getClasse(request.getClasseId()));
        if (request.getActif() != null) etudiant.setActif(request.getActif());
        return etudiantMapper.toResponse(etudiantRepository.save(etudiant));
    }
    @Override @Transactional
    public EtudiantResponse update(UUID id, EtudiantRequest request) {
        Etudiant etudiant = getEtudiant(id);
        assertIdentifiersAvailable(request, etudiant);
        etudiantMapper.updateEntityFromRequest(request, etudiant);
        etudiant.setClasse(getClasse(request.getClasseId()));
        if (request.getActif() != null) etudiant.setActif(request.getActif());
        return etudiantMapper.toResponse(etudiantRepository.save(etudiant));
    }
    @Override @Transactional
    public void setActive(UUID id, boolean actif) { Etudiant etudiant = getEtudiant(id); etudiant.setActif(actif); etudiantRepository.save(etudiant); }

    /**
     * Associe à l'étudiant la référence logique de l'empreinte capturée sur le lecteur.
     *
     * <p>Aucune donnée biométrique n'est reçue ni stockée : le gabarit reste dans la
     * mémoire du capteur AS608.</p>
     */
    @Override
    @Transactional
    public EtudiantResponse enroler(UUID id, String biometricId) {
        Etudiant etudiant = getEtudiant(id);
        if (!etudiant.isActif()) {
            throw new BusinessException("Un étudiant inactif ne peut pas être enrôlé",
                    HttpStatus.CONFLICT);
        }

        String reference = biometricId == null ? "" : biometricId.trim();
        if (reference.isEmpty()) {
            throw new BusinessException("La référence biométrique est obligatoire");
        }

        etudiantRepository.findByBiometricId(reference).ifPresent(autre -> {
            if (!autre.getId().equals(id)) {
                throw new BusinessException(
                        "Cette référence biométrique est déjà attribuée à l'étudiant "
                                + autre.getMatricule(), HttpStatus.CONFLICT);
            }
        });

        etudiant.setBiometricId(reference);
        Etudiant enregistre = etudiantRepository.save(etudiant);
        log.info("Étudiant enrôlé — matricule={}, biometricId={}",
                enregistre.getMatricule(), reference);
        return etudiantMapper.toResponse(enregistre);
    }

    /**
     * Révoque l'enrôlement d'un étudiant.
     *
     * <p>Les présences déjà relevées sont conservées : elles constituent un historique
     * de scolarité, indépendant de la capacité présente à être identifié.</p>
     */
    @Override
    @Transactional
    public EtudiantResponse revoquerEnrolement(UUID id) {
        Etudiant etudiant = getEtudiant(id);
        if (!etudiant.isEnrole()) {
            throw new BusinessException("Cet étudiant n'est pas enrôlé", HttpStatus.CONFLICT);
        }
        log.info("Enrôlement révoqué — matricule={}", etudiant.getMatricule());
        etudiant.setBiometricId(null);
        return etudiantMapper.toResponse(etudiantRepository.save(etudiant));
    }

    /**
     * Crée le compte de connexion mobile d'un étudiant.
     *
     * <p>L'identifiant est l'email de l'étudiant lorsqu'il en a un, sinon une adresse
     * dérivée de son matricule sur le domaine de l'établissement : tous les étudiants
     * n'ont pas d'email personnel, et le matricule est la seule donnée dont on soit sûr.</p>
     */
    @Override
    @Transactional
    public CompteEtudiantResponse ouvrirCompte(UUID id) {
        Etudiant etudiant = getEtudiant(id);

        if (etudiant.isCompteOuvert()) {
            throw new BusinessException(
                    "Cet étudiant dispose déjà d'un accès mobile", HttpStatus.CONFLICT);
        }
        if (!etudiant.isActif()) {
            throw new BusinessException(
                    "Un étudiant inactif ne peut pas recevoir d'accès", HttpStatus.CONFLICT);
        }

        String email = identifiantDeConnexion(etudiant);
        if (utilisateurRepository.existsByEmail(email)) {
            throw new BusinessException(
                    "Un compte utilise déjà l'adresse " + email, HttpStatus.CONFLICT);
        }

        Role roleEtudiant = roleRepository.findByCode(RoleCode.ETUDIANT.name())
                .orElseThrow(() -> new ResourceNotFoundException("Rôle", "code", RoleCode.ETUDIANT));

        String motDePasseClair = GenerateurMotDePasse.generer();

        Utilisateur compte = new Utilisateur();
        compte.setEmail(email);
        compte.setMotDePasse(passwordEncoder.encode(motDePasseClair));
        compte.setNom(etudiant.getNom());
        compte.setPrenom(etudiant.getPrenom());
        compte.setActif(true);
        compte.setRoles(Set.of(roleEtudiant));
        compte = utilisateurRepository.save(compte);

        etudiant.setUtilisateur(compte);
        etudiantRepository.save(etudiant);

        log.info("Accès mobile ouvert pour l'étudiant {} ({})", etudiant.getMatricule(), email);

        return CompteEtudiantResponse.builder()
                .etudiantId(etudiant.getId())
                .matricule(etudiant.getMatricule())
                .nomComplet(etudiant.getPrenom() + " " + etudiant.getNom())
                .utilisateurId(compte.getId())
                .email(email)
                .motDePasseInitial(motDePasseClair)
                .build();
    }

    @Override
    @Transactional
    public EtudiantResponse fermerCompte(UUID id) {
        Etudiant etudiant = getEtudiant(id);
        Utilisateur compte = etudiant.getUtilisateur();
        if (compte == null) {
            throw new BusinessException("Cet étudiant n'a pas d'accès mobile", HttpStatus.CONFLICT);
        }

        // Le lien est retiré avant la suppression : la clé étrangère vit côté étudiant.
        etudiant.setUtilisateur(null);
        etudiantRepository.save(etudiant);
        utilisateurRepository.delete(compte);

        log.info("Accès mobile fermé pour l'étudiant {}", etudiant.getMatricule());
        return etudiantMapper.toResponse(etudiant);
    }

    /**
     * Identifiant de connexion de l'étudiant.
     *
     * <p>Son email s'il en a un ; à défaut une adresse construite sur le matricule et le
     * domaine configuré, de façon à ce que tout étudiant puisse recevoir un accès.</p>
     */
    private String identifiantDeConnexion(Etudiant etudiant) {
        String email = etudiant.getEmail();
        if (email != null && !email.isBlank()) {
            return email.trim().toLowerCase();
        }
        return etudiant.getMatricule().trim().toLowerCase() + "@" + domaineEtudiants;
    }
    private void assertIdentifiersAvailable(EtudiantRequest request, Etudiant current) {
        if ((current == null || !current.getMatricule().equals(request.getMatricule())) && etudiantRepository.existsByMatricule(request.getMatricule())) throw new BusinessException("Un étudiant existe déjà avec ce matricule");
        // Un biometricId absent est desormais legitime : l'unicite ne s'applique
        // qu'aux valeurs reellement renseignees.
        String biometrique = request.getBiometricId();
        if (biometrique != null && !biometrique.isBlank()
                && !biometrique.equals(current == null ? null : current.getBiometricId())
                && etudiantRepository.existsByBiometricId(biometrique)) {
            throw new BusinessException("Cet identifiant biométrique logique est déjà attribué",
                    HttpStatus.CONFLICT);
        }
    }
    private Etudiant getEtudiant(UUID id) { return etudiantRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Étudiant", "id", id)); }
    private Classe getClasse(Long id) { return classeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Classe", "id", id)); }
}
