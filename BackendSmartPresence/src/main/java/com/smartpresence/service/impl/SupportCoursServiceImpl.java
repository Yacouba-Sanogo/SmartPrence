package com.smartpresence.service.impl;

import com.smartpresence.constants.TypeNotification;
import com.smartpresence.dto.response.GestionResponses;
import com.smartpresence.entity.Classe;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.entity.Matiere;
import com.smartpresence.entity.Personnel;
import com.smartpresence.entity.SupportContenu;
import com.smartpresence.entity.SupportCours;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.MatiereRepository;
import com.smartpresence.repository.PersonnelRepository;
import com.smartpresence.repository.SupportContenuRepository;
import com.smartpresence.repository.SupportCoursRepository;
import com.smartpresence.service.NotificationService;
import com.smartpresence.service.SupportCoursService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportCoursServiceImpl implements SupportCoursService {

    private static final long TAILLE_MAX = 10L * 1024 * 1024;

    /**
     * Formats acceptés, et le type servi pour chacun.
     *
     * <p>Le type vient de l'extension, jamais de l'en-tête envoyé par le client : un fichier
     * HTML déclaré « application/pdf » serait sinon servi tel quel aux étudiants.</p>
     */
    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("odt", "application/vnd.oasis.opendocument.text"),
            Map.entry("odp", "application/vnd.oasis.opendocument.presentation"),
            Map.entry("ods", "application/vnd.oasis.opendocument.spreadsheet"),
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("zip", "application/zip"));

    private final EnseignantDuCompte enseignants;
    private final PersonnelRepository personnelRepository;
    private final EtudiantRepository etudiantRepository;
    private final ClasseRepository classeRepository;
    private final MatiereRepository matiereRepository;
    private final SupportCoursRepository repository;
    private final SupportContenuRepository contenuRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public GestionResponses.Support publier(UUID utilisateurId, Long classeId, Long matiereId, String titre,
                                            String description, MultipartFile fichier) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        enseignants.exigerClasse(enseignant, classeId);
        if (titre == null || titre.isBlank()) {
            throw new BusinessException("Le titre est obligatoire");
        }
        if (titre.trim().length() > 160) {
            throw new BusinessException("Le titre ne peut pas dépasser 160 caractères");
        }
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Aucun fichier reçu");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw new BusinessException("Le fichier dépasse 10 Mo", HttpStatus.PAYLOAD_TOO_LARGE);
        }

        String nom = nettoyerNom(fichier.getOriginalFilename());
        String extension = extension(nom);
        String type = TYPES.get(extension);
        if (type == null) {
            throw new BusinessException("Format non accepté (." + extension + "). Formats : "
                    + String.join(", ", TYPES.keySet().stream().sorted().toList()));
        }

        Classe classe = classeRepository.findById(classeId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe", "id", classeId));
        Matiere matiere = matiereRepository.findById(matiereId)
                .orElseThrow(() -> new ResourceNotFoundException("Matière", "id", matiereId));

        byte[] octets;
        try {
            octets = fichier.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Le fichier n'a pas pu être lu", HttpStatus.BAD_REQUEST);
        }

        SupportCours support = new SupportCours();
        support.setClasse(classe);
        support.setMatiere(matiere);
        support.setEnseignant(enseignant);
        support.setTitre(titre.trim());
        support.setDescription(description == null || description.isBlank() ? null : description.trim());
        support.setNomFichier(nom);
        support.setTypeMime(type);
        support.setTaille(octets.length);
        SupportCours enregistre = repository.save(support);

        SupportContenu contenu = new SupportContenu();
        contenu.setSupport(enregistre);
        contenu.setOctets(octets);
        contenuRepository.save(contenu);

        prevenirLesEtudiants(enregistre);
        log.info("Support publié — {} ({} octets), classe={}, enseignant={}", nom, octets.length,
                classe.getCode(), enseignant.getMatricule());
        return versReponse(enregistre);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GestionResponses.Support> mesSupports(UUID utilisateurId, Long classeId) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        enseignants.exigerClasse(enseignant, classeId);
        return repository.findParClasse(classeId, enseignant.getId()).stream().map(this::versReponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GestionResponses.Support> supportsDeMaClasse(UUID utilisateurId) {
        Etudiant etudiant = etudiantDuCompte(utilisateurId);
        if (etudiant.getClasse() == null) {
            return List.of();
        }
        return repository.findParClasse(etudiant.getClasse().getId(), null).stream()
                .map(this::versReponse)
                .toList();
    }

    @Override
    @Transactional
    public void supprimer(UUID utilisateurId, UUID supportId) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        SupportCours support = support(supportId);
        if (!support.getEnseignant().getId().equals(enseignant.getId())) {
            throw new BusinessException("Ce support n'est pas le vôtre", HttpStatus.FORBIDDEN);
        }
        contenuRepository.deleteById(support.getId());
        repository.delete(support);
    }

    @Override
    @Transactional(readOnly = true)
    public Fichier telecharger(UUID utilisateurId, UUID supportId) {
        SupportCours support = support(supportId);
        var agent = personnelRepository.findByUtilisateurId(utilisateurId);
        if (agent.isPresent()) {
            if (!support.getEnseignant().getId().equals(agent.get().getId())) {
                throw new BusinessException("Ce support n'est pas le vôtre", HttpStatus.FORBIDDEN);
            }
        } else {
            Etudiant etudiant = etudiantDuCompte(utilisateurId);
            // Même réponse qu'un support inexistant : l'étudiant d'une autre classe n'a pas à
            // savoir qu'il existe.
            if (etudiant.getClasse() == null
                    || !etudiant.getClasse().getId().equals(support.getClasse().getId())) {
                throw new ResourceNotFoundException("Support", "id", supportId);
            }
        }
        SupportContenu contenu = contenuRepository.findById(supportId)
                .orElseThrow(() -> new ResourceNotFoundException("Support", "id", supportId));
        return new Fichier(support.getNomFichier(), support.getTypeMime(), contenu.getOctets());
    }

    // ------------------------------------------------------------------

    private void prevenirLesEtudiants(SupportCours s) {
        for (Etudiant etudiant : etudiantRepository.findByClasseId(s.getClasse().getId())) {
            if (etudiant.isActif() && etudiant.getUtilisateur() != null) {
                notificationService.createNotification(etudiant.getUtilisateur().getId(),
                        TypeNotification.INFORMATION, "Nouveau support — " + s.getMatiere().getLibelle(),
                        s.getTitre());
            }
        }
    }

    private SupportCours support(UUID id) {
        return repository.findAvecContexte(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support", "id", id));
    }

    private Etudiant etudiantDuCompte(UUID utilisateurId) {
        return etudiantRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new BusinessException(
                        "Ce compte n'est rattaché à aucune fiche étudiant", HttpStatus.FORBIDDEN));
    }

    /** Garde le seul nom du fichier : un chemin envoyé par le client n'a rien à faire ici. */
    private static String nettoyerNom(String brut) {
        String nom = brut == null ? "" : brut.replace('\\', '/');
        nom = nom.substring(nom.lastIndexOf('/') + 1).trim();
        if (nom.isEmpty()) {
            throw new BusinessException("Le fichier n'a pas de nom");
        }
        return nom.length() > 255 ? nom.substring(nom.length() - 255) : nom;
    }

    private static String extension(String nom) {
        int point = nom.lastIndexOf('.');
        return point < 0 ? "" : nom.substring(point + 1).toLowerCase(Locale.ROOT);
    }

    private GestionResponses.Support versReponse(SupportCours s) {
        Personnel e = s.getEnseignant();
        return new GestionResponses.Support(s.getId(), s.getClasse().getId(), s.getClasse().getCode(),
                s.getMatiere().getId(), s.getMatiere().getLibelle(), (e.getPrenom() + " " + e.getNom()).trim(),
                s.getTitre(), s.getDescription(), s.getNomFichier(), s.getTypeMime(), s.getTaille(),
                s.getCreatedAt());
    }
}
