package com.smartpresence.service.impl;

import com.smartpresence.constants.StatutPresence;
import com.smartpresence.constants.TypePersonnel;
import com.smartpresence.dto.request.PresenceSearchCriteria;
import com.smartpresence.dto.response.ClasseResponse;
import com.smartpresence.dto.response.EffectifEtudiantResponse;
import com.smartpresence.dto.response.FeuilleSeanceResponse;
import com.smartpresence.dto.response.JustificationAbsenceResponse;
import com.smartpresence.dto.response.PagedResponse;
import com.smartpresence.dto.response.PresenceResponse;
import com.smartpresence.dto.response.ProfilResponse;
import com.smartpresence.dto.response.SeanceResponse;
import com.smartpresence.dto.response.StatistiquesEtudiantResponse;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.entity.Personnel;
import com.smartpresence.entity.Presence;
import com.smartpresence.entity.Seance;
import com.smartpresence.entity.Role;
import com.smartpresence.entity.Utilisateur;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.ClasseMapper;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.PresenceRepository;
import com.smartpresence.repository.SeanceRepository;
import com.smartpresence.repository.PersonnelRepository;
import com.smartpresence.repository.UtilisateurRepository;
import com.smartpresence.service.JustificationService;
import com.smartpresence.service.PresenceService;
import com.smartpresence.service.ProfilService;
import com.smartpresence.service.StatistiquesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfilServiceImpl implements ProfilService {

    private final UtilisateurRepository utilisateurRepository;
    private final EtudiantRepository etudiantRepository;
    private final PersonnelRepository personnelRepository;
    private final PresenceService presenceService;
    private final StatistiquesService statistiquesService;
    private final JustificationService justificationService;
    private final ClasseRepository classeRepository;
    private final SeanceRepository seanceRepository;
    private final PresenceRepository presenceRepository;
    private final ClasseMapper classeMapper;

    @Override
    @Transactional(readOnly = true)
    public ProfilResponse profil(UUID utilisateurId) {
        Utilisateur compte = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", utilisateurId));

        Set<String> roles = compte.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());

        ProfilResponse.ProfilResponseBuilder profil = ProfilResponse.builder()
                .utilisateurId(compte.getId())
                .email(compte.getEmail())
                .nom(compte.getNom())
                .prenom(compte.getPrenom())
                .roles(roles)
                .typeProfil("AUCUN");

        Optional<Etudiant> etudiant = etudiantRepository.findByUtilisateurId(utilisateurId);
        if (etudiant.isPresent()) {
            Etudiant e = etudiant.get();
            return profil
                    .typeProfil("ETUDIANT")
                    .profilId(e.getId())
                    .matricule(e.getMatricule())
                    .classeId(e.getClasse().getId())
                    .classeCode(e.getClasse().getCode())
                    .classeLibelle(e.getClasse().getLibelle())
                    .promotionLibelle(e.getClasse().getPromotion() == null
                            ? null : e.getClasse().getPromotion().getLibelle())
                    .build();
        }

        // Un enseignant est un agent de catégorie ENSEIGNANT : une seule fiche, une
        // seule résolution. La catégorie distingue les deux profils métier.
        Optional<Personnel> personnel = personnelRepository.findByUtilisateurId(utilisateurId);
        if (personnel.isPresent()) {
            Personnel p = personnel.get();
            boolean estEnseignant = p.getType() == TypePersonnel.ENSEIGNANT;
            return profil
                    .typeProfil(estEnseignant ? "ENSEIGNANT" : "PERSONNEL")
                    .profilId(p.getId())
                    .matricule(p.getMatricule())
                    .service(p.getService())
                    .build();
        }

        // Aucun profil métier : c'est le cas normal d'un administrateur.
        return profil.build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PresenceResponse> mesPresences(UUID utilisateurId, LocalDate debut,
                                                        LocalDate fin, int page, int taille) {
        Etudiant etudiant = etudiantDuCompte(utilisateurId);
        PresenceSearchCriteria criteres = PresenceSearchCriteria.builder()
                .etudiantId(etudiant.getId())
                .dateDebut(debut)
                .dateFin(fin)
                .page(page)
                .size(taille)
                .build();
        return presenceService.search(criteres);
    }

    @Override
    @Transactional(readOnly = true)
    public StatistiquesEtudiantResponse mesStatistiques(UUID utilisateurId) {
        return statistiquesService.getByEtudiant(etudiantDuCompte(utilisateurId).getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JustificationAbsenceResponse> mesJustificatifs(UUID utilisateurId) {
        return justificationService.findByEtudiant(etudiantDuCompte(utilisateurId).getId());
    }

    // ------------------------------------------------------------------
    // Espace enseignant
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<ClasseResponse> mesClasses(UUID utilisateurId) {
        Personnel enseignant = enseignantDuCompte(utilisateurId);
        return classeMapper.toResponseList(classeRepository.findParEnseignant(enseignant.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeanceResponse> mesSeances(UUID utilisateurId, LocalDate jour) {
        Personnel enseignant = enseignantDuCompte(utilisateurId);
        ZoneId zone = ZoneId.systemDefault();
        Instant debut = jour.atStartOfDay(zone).toInstant();
        Instant fin = jour.plusDays(1).atStartOfDay(zone).toInstant();
        return seanceRepository.findParEnseignant(enseignant.getId(), debut, fin).stream()
                .map(this::versSeanceResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EffectifEtudiantResponse> mesEtudiants(UUID utilisateurId, Long classeId) {
        Personnel enseignant = enseignantDuCompte(utilisateurId);

        boolean intervientDansLaClasse = classeRepository.findParEnseignant(enseignant.getId())
                .stream()
                .anyMatch(classe -> classe.getId().equals(classeId));
        if (!intervientDansLaClasse) {
            throw new BusinessException(
                    "Vous n'intervenez pas dans cette classe", HttpStatus.FORBIDDEN);
        }

        return etudiantRepository.findByClasseId(classeId).stream()
                .sorted(Comparator.comparing(Etudiant::getNom).thenComparing(Etudiant::getPrenom))
                .map(etudiant -> EffectifEtudiantResponse.builder()
                        .id(etudiant.getId())
                        .matricule(etudiant.getMatricule())
                        .nom(etudiant.getNom())
                        .prenom(etudiant.getPrenom())
                        .enrole(etudiant.isEnrole())
                        .actif(etudiant.isActif())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FeuilleSeanceResponse feuilleDeSeance(UUID utilisateurId, UUID seanceId) {
        Personnel enseignant = enseignantDuCompte(utilisateurId);
        Seance seance = seanceRepository.findAvecContexte(seanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Seance", "id", seanceId));

        if (!seance.getEnseignant().getId().equals(enseignant.getId())) {
            throw new BusinessException(
                    "Cette seance n'est pas la votre", HttpStatus.FORBIDDEN);
        }

        // La feuille se construit en partant de l'effectif de la classe, pas des
        // releves : autrement, un etudiant jamais identifie n'apparaitrait pas du tout.
        Map<UUID, Presence> relevesParEtudiant = presenceRepository.findBySeanceId(seanceId)
                .stream()
                .collect(Collectors.toMap(
                        presence -> presence.getEtudiant().getId(),
                        presence -> presence,
                        // Deux releves pour un meme etudiant : on retient le plus matinal.
                        (a, b) -> a.getHeurePresence().isBefore(b.getHeurePresence()) ? a : b));

        List<Etudiant> effectif = etudiantRepository.findByClasseId(seance.getClasse().getId());

        List<FeuilleSeanceResponse.LigneFeuilleResponse> lignes = effectif.stream()
                .sorted(Comparator.comparing(Etudiant::getNom).thenComparing(Etudiant::getPrenom))
                .map(etudiant -> versLigne(etudiant, relevesParEtudiant.get(etudiant.getId())))
                .toList();

        return FeuilleSeanceResponse.builder()
                .seanceId(seance.getId())
                .matiereLibelle(seance.getMatiere().getLibelle())
                .classeId(seance.getClasse().getId())
                .classeCode(seance.getClasse().getCode())
                .classeLibelle(seance.getClasse().getLibelle())
                .salleLibelle(seance.getSalle() == null ? null : seance.getSalle().getLibelle())
                .debut(seance.getDebut())
                .fin(seance.getFin())
                .statut(seance.getStatut())
                .effectif(lignes.size())
                .presents((int) lignes.stream()
                        .filter(l -> l.getStatut() == StatutPresence.PRESENT).count())
                .retards((int) lignes.stream()
                        .filter(l -> l.getStatut() == StatutPresence.RETARD).count())
                .absents((int) lignes.stream()
                        .filter(l -> l.getStatut() == StatutPresence.ABSENT).count())
                .absentsNonEnroles((int) lignes.stream()
                        .filter(l -> l.getStatut() == StatutPresence.ABSENT && !l.isEnrole()).count())
                .lignes(lignes)
                .build();
    }

    private FeuilleSeanceResponse.LigneFeuilleResponse versLigne(Etudiant etudiant, Presence releve) {
        return FeuilleSeanceResponse.LigneFeuilleResponse.builder()
                .etudiantId(etudiant.getId())
                .matricule(etudiant.getMatricule())
                .nom(etudiant.getNom())
                .prenom(etudiant.getPrenom())
                .enrole(etudiant.isEnrole())
                .statut(releve == null ? StatutPresence.ABSENT : releve.getStatut())
                .heure(releve == null ? null : releve.getHeurePresence())
                .source(releve == null ? null : releve.getSource())
                .build();
    }

    private SeanceResponse versSeanceResponse(Seance s) {
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
                .build();
    }

    /**
     * Fiche d'enseignant rattachee a un compte.
     *
     * <p>Exige la categorie {@code ENSEIGNANT} : un agent administratif authentifie ne
     * doit pas acceder aux feuilles de presence, meme s'il possede une fiche personnel.</p>
     */
    private Personnel enseignantDuCompte(UUID utilisateurId) {
        Personnel personnel = personnelRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new BusinessException(
                        "Ce compte n'est rattache a aucune fiche d'agent", HttpStatus.FORBIDDEN));
        if (personnel.getType() != TypePersonnel.ENSEIGNANT) {
            throw new BusinessException(
                    "Ce compte n'est pas rattache a un enseignant", HttpStatus.FORBIDDEN);
        }
        return personnel;
    }

    @Override
    @Transactional(readOnly = true)
    public Etudiant etudiantDuCompte(UUID utilisateurId) {
        return etudiantRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new BusinessException(
                        "Ce compte n'est rattaché à aucune fiche étudiant", HttpStatus.FORBIDDEN));
    }
}
