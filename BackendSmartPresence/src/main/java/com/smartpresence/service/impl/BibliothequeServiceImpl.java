package com.smartpresence.service.impl;

import com.smartpresence.constants.StatutDocument;
import com.smartpresence.constants.StatutEmprunt;
import com.smartpresence.constants.TypeDocument;
import com.smartpresence.constants.TypeNotification;
import com.smartpresence.dto.request.BibliothequeRequests;
import com.smartpresence.dto.response.BibliothequeResponses;
import com.smartpresence.entity.DocumentBibliotheque;
import com.smartpresence.entity.DocumentContenu;
import com.smartpresence.entity.Emprunt;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.entity.Filiere;
import com.smartpresence.entity.Livre;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.repository.DocumentBibliothequeRepository;
import com.smartpresence.repository.DocumentContenuRepository;
import com.smartpresence.repository.EmpruntRepository;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.FiliereRepository;
import com.smartpresence.repository.LivreRepository;
import com.smartpresence.repository.PersonnelRepository;
import com.smartpresence.service.BibliothequeService;
import com.smartpresence.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BibliothequeServiceImpl implements BibliothequeService {

    private static final long TAILLE_MAX = 25L * 1024 * 1024;
    static final int DUREE_PAR_DEFAUT = 14;
    static final int EMPRUNTS_MAX = 3;
    static final int PROLONGATIONS_MAX = 2;
    private static final int INTERVALLE_RAPPEL_RETARD = 7;

    /** Le type servi vient de l'extension, jamais de l'en-tête du client (voir les supports de cours). */
    private static final Map<String, String> TYPES = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "odt", "application/vnd.oasis.opendocument.text",
            "ppt", "application/vnd.ms-powerpoint",
            "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "zip", "application/zip",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg");

    private final DocumentBibliothequeRepository documents;
    private final DocumentContenuRepository contenus;
    private final LivreRepository livres;
    private final EmpruntRepository emprunts;
    private final FiliereRepository filieres;
    private final EtudiantRepository etudiants;
    private final PersonnelRepository personnel;
    private final NotificationService notifications;

    // ===================================================================
    // Documents numériques
    // ===================================================================

    @Override
    @Transactional(readOnly = true)
    public List<BibliothequeResponses.Document> documents(TypeDocument type, Long filiereId, Integer annee,
                                                          String recherche) {
        String mot = normaliser(recherche);
        return documents.findParStatut(StatutDocument.PUBLIE).stream()
                .filter(d -> type == null || d.getType() == type)
                .filter(d -> filiereId == null || (d.getFiliere() != null && filiereId.equals(d.getFiliere().getId())))
                .filter(d -> annee == null || annee.equals(d.getAnnee()))
                .filter(d -> mot.isEmpty() || normaliser(d.getTitre() + " " + d.getAuteur() + " " + d.getMatiere()
                        + " " + d.getDescription()).contains(mot))
                .map(this::versReponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliothequeResponses.Document> documentsAModerer() {
        return documents.findParStatut(StatutDocument.EN_ATTENTE).stream().map(this::versReponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliothequeResponses.Document> mesDepots(UUID utilisateurId) {
        return documents.findDuDeposant(utilisateurId).stream().map(this::versReponse).toList();
    }

    @Override
    @Transactional
    public BibliothequeResponses.Document deposer(UUID utilisateurId, boolean gestionnaire, TypeDocument type,
                                                  String titre, String auteur, String matiere, Long filiereId,
                                                  Integer annee, String description, MultipartFile fichier) {
        if (type == null) {
            throw new BusinessException("Précisez s'il s'agit d'un mémoire ou d'un sujet");
        }
        String titreNet = exiger(titre, "Le titre est obligatoire", 160);
        String auteurNet = limiter(auteur, 150, "L'auteur");
        String matiereNet = limiter(matiere, 150, "La matière");
        if (type == TypeDocument.MEMOIRE && auteurNet == null) {
            throw new BusinessException("L'auteur du mémoire est obligatoire");
        }
        if (type == TypeDocument.SUJET && matiereNet == null) {
            throw new BusinessException("La matière du sujet est obligatoire");
        }
        if (annee != null && (annee < 1950 || annee > LocalDate.now().getYear() + 1)) {
            throw new BusinessException("Année invalide");
        }
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Aucun fichier reçu");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw new BusinessException("Le fichier dépasse 25 Mo", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String nom = nettoyerNom(fichier.getOriginalFilename());
        String extension = extension(nom);
        String typeMime = TYPES.get(extension);
        if (typeMime == null) {
            throw new BusinessException("Format non accepté (." + extension + "). Formats : "
                    + String.join(", ", TYPES.keySet().stream().sorted().toList()));
        }
        Filiere filiere = filiereId == null ? null : filieres.findById(filiereId)
                .orElseThrow(() -> new ResourceNotFoundException("Filière", "id", filiereId));

        byte[] octets;
        try {
            octets = fichier.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Le fichier n'a pas pu être lu", HttpStatus.BAD_REQUEST);
        }

        DocumentBibliotheque doc = new DocumentBibliotheque();
        doc.setType(type);
        doc.setTitre(titreNet);
        doc.setAuteur(auteurNet);
        doc.setMatiere(matiereNet);
        doc.setFiliere(filiere);
        doc.setAnnee(annee);
        doc.setDescription(limiter(description, 500, "La description"));
        doc.setNomFichier(nom);
        doc.setTypeMime(typeMime);
        doc.setTaille(octets.length);
        doc.setStatut(gestionnaire ? StatutDocument.PUBLIE : StatutDocument.EN_ATTENTE);
        doc.setDeposeParId(utilisateurId);
        doc.setDeposeParNom(nomDuDeposant(utilisateurId, gestionnaire));
        DocumentBibliotheque enregistre = documents.save(doc);

        DocumentContenu contenu = new DocumentContenu();
        contenu.setDocument(enregistre);
        contenu.setOctets(octets);
        contenus.save(contenu);

        log.info("Bibliothèque — document déposé : {} ({}, {} octets), statut={}", titreNet, type, octets.length,
                enregistre.getStatut());
        return versReponse(enregistre);
    }

    @Override
    @Transactional
    public BibliothequeResponses.Document decider(UUID documentId, BibliothequeRequests.Decision decision) {
        DocumentBibliotheque doc = document(documentId);
        if (doc.getStatut() != StatutDocument.EN_ATTENTE) {
            throw new BusinessException("Ce document a déjà reçu une décision", HttpStatus.CONFLICT);
        }
        boolean accepte = Boolean.TRUE.equals(decision.accepte());
        String motif = decision.motif() == null ? "" : decision.motif().trim();
        if (!accepte && motif.isEmpty()) {
            throw new BusinessException("Indiquez le motif du refus : le déposant le verra");
        }
        doc.setStatut(accepte ? StatutDocument.PUBLIE : StatutDocument.REFUSE);
        doc.setMotifRefus(accepte ? null : motif);
        documents.save(doc);

        notifications.createNotification(doc.getDeposeParId(), TypeNotification.INFORMATION,
                accepte ? "Document publié" : "Document refusé",
                accepte ? "« " + doc.getTitre() + " » est maintenant dans la bibliothèque."
                        : "« " + doc.getTitre() + " » n'a pas été retenu : " + motif);
        return versReponse(doc);
    }

    @Override
    @Transactional
    public void retirer(UUID utilisateurId, boolean gestionnaire, UUID documentId) {
        DocumentBibliotheque doc = document(documentId);
        boolean auteur = doc.getDeposeParId().equals(utilisateurId);
        if (!gestionnaire) {
            if (!auteur) {
                throw new ResourceNotFoundException("Document", "id", documentId);
            }
            if (doc.getStatut() == StatutDocument.PUBLIE) {
                throw new BusinessException("Un document publié ne peut être retiré que par l'administration",
                        HttpStatus.FORBIDDEN);
            }
        }
        contenus.deleteById(doc.getId());
        documents.delete(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public Fichier telecharger(UUID utilisateurId, boolean gestionnaire, UUID documentId) {
        DocumentBibliotheque doc = document(documentId);
        boolean visible = doc.getStatut() == StatutDocument.PUBLIE || gestionnaire
                || doc.getDeposeParId().equals(utilisateurId);
        // Même réponse qu'un document inexistant : un dépôt en attente n'a pas à être deviné.
        if (!visible) {
            throw new ResourceNotFoundException("Document", "id", documentId);
        }
        DocumentContenu contenu = contenus.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        return new Fichier(doc.getNomFichier(), doc.getTypeMime(), contenu.getOctets());
    }

    // ===================================================================
    // Livres
    // ===================================================================

    @Override
    @Transactional(readOnly = true)
    public List<BibliothequeResponses.Livre> livres(Long filiereId, String recherche) {
        String mot = normaliser(recherche);
        return livres.findTous().stream()
                .filter(l -> filiereId == null || (l.getFiliere() != null && filiereId.equals(l.getFiliere().getId())))
                .filter(l -> mot.isEmpty() || normaliser(l.getTitre() + " " + l.getAuteur() + " " + l.getIsbn()
                        + " " + l.getCategorie()).contains(mot))
                .map(this::versReponse)
                .toList();
    }

    @Override
    @Transactional
    public BibliothequeResponses.Livre creerLivre(BibliothequeRequests.Livre requete) {
        Livre livre = new Livre();
        appliquer(livre, requete);
        return versReponse(livres.save(livre));
    }

    @Override
    @Transactional
    public BibliothequeResponses.Livre modifierLivre(UUID id, BibliothequeRequests.Livre requete) {
        Livre livre = livre(id);
        long sortis = emprunts.countByLivreIdAndRetourneLeIsNull(id);
        if (requete.exemplaires() < sortis) {
            throw new BusinessException("Il y a " + sortis + " exemplaire(s) actuellement emprunté(s) : "
                    + "le total ne peut pas descendre en dessous", HttpStatus.CONFLICT);
        }
        appliquer(livre, requete);
        return versReponse(livres.save(livre));
    }

    @Override
    @Transactional
    public void supprimerLivre(UUID id) {
        Livre livre = livre(id);
        if (emprunts.existsByLivreId(id)) {
            throw new BusinessException("Ce livre a un historique d'emprunts : il ne peut pas être supprimé",
                    HttpStatus.CONFLICT);
        }
        livres.delete(livre);
    }

    // ===================================================================
    // Emprunts
    // ===================================================================

    @Override
    @Transactional(readOnly = true)
    public List<BibliothequeResponses.Emprunt> emprunts() {
        LocalDate jour = LocalDate.now();
        return emprunts.findTous().stream().map(e -> versReponse(e, jour)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliothequeResponses.Emprunt> mesEmprunts(UUID utilisateurId) {
        Etudiant etudiant = etudiants.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new BusinessException(
                        "Ce compte n'est rattaché à aucune fiche étudiant", HttpStatus.FORBIDDEN));
        LocalDate jour = LocalDate.now();
        return emprunts.findDeLEtudiant(etudiant.getId()).stream().map(e -> versReponse(e, jour)).toList();
    }

    @Override
    @Transactional
    public BibliothequeResponses.Emprunt emprunter(BibliothequeRequests.Emprunt requete) {
        LocalDate jour = LocalDate.now();
        Livre livre = livre(requete.livreId());
        Etudiant etudiant = etudiants.findById(requete.etudiantId())
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant", "id", requete.etudiantId()));
        if (!etudiant.isActif()) {
            throw new BusinessException("Cet étudiant est désactivé", HttpStatus.CONFLICT);
        }
        long sortis = emprunts.countByLivreIdAndRetourneLeIsNull(livre.getId());
        if (sortis >= livre.getExemplaires()) {
            throw new BusinessException("Tous les exemplaires de ce livre sont déjà empruntés", HttpStatus.CONFLICT);
        }
        List<Emprunt> ouverts = emprunts.findDeLEtudiant(etudiant.getId()).stream()
                .filter(e -> e.getRetourneLe() == null).toList();
        if (ouverts.stream().anyMatch(e -> e.statutAu(jour) == StatutEmprunt.EN_RETARD)) {
            throw new BusinessException("Cet étudiant a un livre en retard : il doit d'abord le rapporter",
                    HttpStatus.CONFLICT);
        }
        if (ouverts.size() >= EMPRUNTS_MAX) {
            throw new BusinessException("Cet étudiant a déjà " + EMPRUNTS_MAX + " livres empruntés",
                    HttpStatus.CONFLICT);
        }
        if (ouverts.stream().anyMatch(e -> e.getLivre().getId().equals(livre.getId()))) {
            throw new BusinessException("Cet étudiant a déjà ce livre", HttpStatus.CONFLICT);
        }

        int duree = requete.dureeJours() == null ? DUREE_PAR_DEFAUT : requete.dureeJours();
        Emprunt emprunt = new Emprunt();
        emprunt.setLivre(livre);
        emprunt.setEtudiant(etudiant);
        emprunt.setEmprunteLe(jour);
        emprunt.setRetourPrevu(jour.plusDays(duree));
        Emprunt enregistre = emprunts.save(emprunt);

        prevenir(etudiant, TypeNotification.INFORMATION, "Livre emprunté",
                "« " + livre.getTitre() + " » est à rendre avant le " + enregistre.getRetourPrevu() + ".");
        return versReponse(enregistre, jour);
    }

    @Override
    @Transactional
    public BibliothequeResponses.Emprunt retourner(UUID empruntId) {
        Emprunt emprunt = emprunt(empruntId);
        if (emprunt.getRetourneLe() != null) {
            throw new BusinessException("Ce livre est déjà rendu", HttpStatus.CONFLICT);
        }
        LocalDate jour = LocalDate.now();
        emprunt.setRetourneLe(jour);
        emprunts.save(emprunt);
        prevenir(emprunt.getEtudiant(), TypeNotification.INFORMATION, "Livre rendu",
                "Retour enregistré : « " + emprunt.getLivre().getTitre() + " ». Merci.");
        return versReponse(emprunt, jour);
    }

    @Override
    @Transactional
    public BibliothequeResponses.Emprunt prolonger(UUID empruntId, int jours) {
        Emprunt emprunt = emprunt(empruntId);
        LocalDate jour = LocalDate.now();
        if (emprunt.getRetourneLe() != null) {
            throw new BusinessException("Ce livre est déjà rendu", HttpStatus.CONFLICT);
        }
        if (emprunt.statutAu(jour) == StatutEmprunt.EN_RETARD) {
            throw new BusinessException("Le livre est en retard : il doit être rapporté, pas prolongé",
                    HttpStatus.CONFLICT);
        }
        if (emprunt.getProlongations() >= PROLONGATIONS_MAX) {
            throw new BusinessException("Cet emprunt a déjà été prolongé " + PROLONGATIONS_MAX + " fois",
                    HttpStatus.CONFLICT);
        }
        emprunt.setRetourPrevu(emprunt.getRetourPrevu().plusDays(jours));
        emprunt.setProlongations(emprunt.getProlongations() + 1);
        emprunt.setDernierRappelLe(null);
        emprunts.save(emprunt);
        prevenir(emprunt.getEtudiant(), TypeNotification.INFORMATION, "Emprunt prolongé",
                "« " + emprunt.getLivre().getTitre() + " » est à rendre avant le " + emprunt.getRetourPrevu() + ".");
        return versReponse(emprunt, jour);
    }

    @Override
    @Transactional(readOnly = true)
    public BibliothequeResponses.Synthese synthese() {
        LocalDate jour = LocalDate.now();
        List<Emprunt> ouverts = emprunts.findOuverts();
        long enRetard = ouverts.stream().filter(e -> e.statutAu(jour) == StatutEmprunt.EN_RETARD).count();
        return new BibliothequeResponses.Synthese(documents.countByStatut(StatutDocument.EN_ATTENTE),
                ouverts.size() - enRetard, enRetard);
    }

    @Override
    @Transactional
    public int envoyerRappels() {
        LocalDate jour = LocalDate.now();
        int envoyes = 0;
        for (Emprunt e : emprunts.findOuverts()) {
            if (jour.equals(e.getDernierRappelLe())) {
                continue;
            }
            String titre = e.getLivre().getTitre();
            if (e.statutAu(jour) == StatutEmprunt.EN_RETARD) {
                boolean recent = e.getDernierRappelLe() != null
                        && ChronoUnit.DAYS.between(e.getDernierRappelLe(), jour) < INTERVALLE_RAPPEL_RETARD;
                if (recent) {
                    continue;
                }
                prevenir(e.getEtudiant(), TypeNotification.RETARD, "Livre en retard",
                        "« " + titre + " » devait être rendu le " + e.getRetourPrevu() + " ("
                                + e.joursDeRetardAu(jour) + " jour(s) de retard). Rapportez-le à la bibliothèque.");
            } else if (e.getRetourPrevu().equals(jour.plusDays(1))) {
                prevenir(e.getEtudiant(), TypeNotification.INFORMATION, "Livre à rendre demain",
                        "« " + titre + " » est à rapporter à la bibliothèque demain.");
            } else {
                continue;
            }
            e.setDernierRappelLe(jour);
            emprunts.save(e);
            envoyes++;
        }
        if (envoyes > 0) {
            log.info("Bibliothèque — {} rappel(s) envoyé(s)", envoyes);
        }
        return envoyes;
    }

    // ===================================================================

    private void prevenir(Etudiant etudiant, TypeNotification type, String titre, String message) {
        if (etudiant.getUtilisateur() != null) {
            notifications.createNotification(etudiant.getUtilisateur().getId(), type, titre, message);
        }
    }

    private void appliquer(Livre livre, BibliothequeRequests.Livre r) {
        livre.setTitre(r.titre().trim());
        livre.setAuteur(r.auteur().trim());
        livre.setIsbn(vide(r.isbn()));
        livre.setCategorie(vide(r.categorie()));
        livre.setResume(vide(r.resume()));
        livre.setEmplacement(vide(r.emplacement()));
        livre.setExemplaires(r.exemplaires());
        livre.setFiliere(r.filiereId() == null ? null : filieres.findById(r.filiereId())
                .orElseThrow(() -> new ResourceNotFoundException("Filière", "id", r.filiereId())));
    }

    private DocumentBibliotheque document(UUID id) {
        return documents.findAvecFiliere(id).orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
    }

    private Livre livre(UUID id) {
        return livres.findById(id).orElseThrow(() -> new ResourceNotFoundException("Livre", "id", id));
    }

    private Emprunt emprunt(UUID id) {
        return emprunts.findAvecContexte(id).orElseThrow(() -> new ResourceNotFoundException("Emprunt", "id", id));
    }

    private String nomDuDeposant(UUID utilisateurId, boolean gestionnaire) {
        return etudiants.findByUtilisateurId(utilisateurId)
                .map(e -> (e.getPrenom() + " " + e.getNom()).trim())
                .or(() -> personnel.findByUtilisateurId(utilisateurId)
                        .map(p -> (p.getPrenom() + " " + p.getNom()).trim()))
                .orElse(gestionnaire ? "Administration" : "Étudiant");
    }

    private BibliothequeResponses.Document versReponse(DocumentBibliotheque d) {
        Filiere f = d.getFiliere();
        return new BibliothequeResponses.Document(d.getId(), d.getType(), d.getTitre(), d.getAuteur(),
                d.getMatiere(), f == null ? null : f.getId(), f == null ? null : f.getLibelle(), d.getAnnee(),
                d.getDescription(), d.getNomFichier(), d.getTypeMime(), d.getTaille(), d.getStatut(),
                d.getMotifRefus(), d.getDeposeParNom(), d.getCreatedAt());
    }

    private BibliothequeResponses.Livre versReponse(Livre l) {
        long sortis = emprunts.countByLivreIdAndRetourneLeIsNull(l.getId());
        Filiere f = l.getFiliere();
        return new BibliothequeResponses.Livre(l.getId(), l.getTitre(), l.getAuteur(), l.getIsbn(),
                l.getCategorie(), f == null ? null : f.getId(), f == null ? null : f.getLibelle(), l.getResume(),
                l.getEmplacement(), l.getExemplaires(), (int) Math.max(0, l.getExemplaires() - sortis));
    }

    private BibliothequeResponses.Emprunt versReponse(Emprunt e, LocalDate jour) {
        Etudiant et = e.getEtudiant();
        return new BibliothequeResponses.Emprunt(e.getId(), e.getLivre().getId(), e.getLivre().getTitre(),
                e.getLivre().getAuteur(), et.getId(), (et.getPrenom() + " " + et.getNom()).trim(),
                et.getMatricule(), e.getEmprunteLe(), e.getRetourPrevu(), e.getRetourneLe(), e.statutAu(jour),
                e.joursDeRetardAu(jour), e.getProlongations());
    }

    private static String exiger(String valeur, String message, int max) {
        if (valeur == null || valeur.isBlank()) {
            throw new BusinessException(message);
        }
        String net = valeur.trim();
        if (net.length() > max) {
            throw new BusinessException("Le titre ne peut pas dépasser " + max + " caractères");
        }
        return net;
    }

    private static String limiter(String valeur, int max, String nom) {
        String net = vide(valeur);
        if (net != null && net.length() > max) {
            throw new BusinessException(nom + " ne peut pas dépasser " + max + " caractères");
        }
        return net;
    }

    private static String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    private static String normaliser(String texte) {
        if (texte == null) {
            return "";
        }
        return java.text.Normalizer.normalize(texte, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
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
}
