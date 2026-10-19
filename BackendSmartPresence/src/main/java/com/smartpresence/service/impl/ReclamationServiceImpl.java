package com.smartpresence.service.impl;

import com.smartpresence.constants.StatutNote;
import com.smartpresence.constants.StatutReclamation;
import com.smartpresence.constants.TypeNotification;
import com.smartpresence.dto.request.GestionRequests;
import com.smartpresence.dto.response.GestionResponses;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.entity.Note;
import com.smartpresence.entity.Personnel;
import com.smartpresence.entity.Reclamation;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.NoteRepository;
import com.smartpresence.repository.ReclamationRepository;
import com.smartpresence.service.NotificationService;
import com.smartpresence.service.ReclamationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReclamationServiceImpl implements ReclamationService {

    private final EnseignantDuCompte enseignants;
    private final EtudiantRepository etudiantRepository;
    private final NoteRepository noteRepository;
    private final ReclamationRepository repository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public GestionResponses.Reclamation contester(UUID utilisateurId, UUID noteId,
                                                  GestionRequests.Contestation requete) {
        Etudiant etudiant = etudiantDuCompte(utilisateurId);
        Note note = noteRepository.findAvecContexte(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note", "id", noteId));

        // Comparer les identifiants : un étudiant ne conteste que ses propres notes, et
        // l'existence d'une note d'autrui ne doit pas se déduire de la réponse.
        if (!note.getEtudiant().getId().equals(etudiant.getId()) || note.getStatut() != StatutNote.VALIDEE) {
            throw new ResourceNotFoundException("Note", "id", noteId);
        }
        if (repository.existsByNoteIdAndStatut(noteId, StatutReclamation.OUVERTE)) {
            throw new BusinessException(
                    "Une contestation est déjà en cours sur cette note", HttpStatus.CONFLICT);
        }

        Reclamation reclamation = new Reclamation();
        reclamation.setNote(note);
        reclamation.setEtudiant(etudiant);
        reclamation.setEnseignant(note.getEnseignant());
        reclamation.setMotif(requete.motif().trim());
        Reclamation enregistree = repository.save(reclamation);

        notificationService.notifierEnseignant(note.getEnseignant(), TypeNotification.INFORMATION,
                "Note contestée — " + note.getMatiere().getLibelle(),
                etudiant.getPrenom() + " " + etudiant.getNom() + " conteste « " + note.getLibelle() + " ».");
        log.info("Note contestée — étudiant={}, note={}", etudiant.getMatricule(), noteId);
        return versReponse(enregistree);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GestionResponses.Reclamation> mesContestations(UUID utilisateurId) {
        return repository.findParEtudiant(etudiantDuCompte(utilisateurId).getId()).stream()
                .map(this::versReponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GestionResponses.Reclamation> recues(UUID utilisateurId) {
        return repository.findParEnseignant(enseignants.resoudre(utilisateurId).getId()).stream()
                .map(this::versReponse)
                .toList();
    }

    @Override
    @Transactional
    public GestionResponses.Reclamation repondre(UUID utilisateurId, UUID reclamationId,
                                                 GestionRequests.ReponseReclamation reponse) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        Reclamation reclamation = repository.findAvecContexte(reclamationId)
                .orElseThrow(() -> new ResourceNotFoundException("Réclamation", "id", reclamationId));
        if (!reclamation.getEnseignant().getId().equals(enseignant.getId())) {
            throw new BusinessException("Cette réclamation ne vous est pas adressée", HttpStatus.FORBIDDEN);
        }
        if (reclamation.getStatut() != StatutReclamation.OUVERTE) {
            throw new BusinessException("Cette réclamation a déjà reçu une réponse", HttpStatus.CONFLICT);
        }
        if (!reponse.accepter() && reponse.valeurCorrigee() != null) {
            throw new BusinessException("Une réclamation rejetée ne corrige pas la note");
        }

        Note note = reclamation.getNote();
        if (reponse.accepter() && reponse.valeurCorrigee() != null
                && reponse.valeurCorrigee().compareTo(note.getValeur()) != 0) {
            // La note est publiée et verrouillée : seule une réclamation acceptée autorise
            // l'auteur à la corriger, et la réclamation garde les deux valeurs.
            reclamation.setValeurAvant(note.getValeur());
            reclamation.setValeurApres(reponse.valeurCorrigee());
            note.setValeur(reponse.valeurCorrigee());
            noteRepository.save(note);
        }

        reclamation.setStatut(reponse.accepter() ? StatutReclamation.ACCEPTEE : StatutReclamation.REJETEE);
        reclamation.setReponse(reponse.reponse().trim());
        reclamation.setTraiteeLe(Instant.now());
        Reclamation traitee = repository.save(reclamation);

        Etudiant etudiant = traitee.getEtudiant();
        if (etudiant.getUtilisateur() != null) {
            notificationService.createNotification(etudiant.getUtilisateur().getId(), TypeNotification.INFORMATION,
                    reponse.accepter() ? "Réclamation acceptée" : "Réclamation rejetée",
                    note.getMatiere().getLibelle() + " — " + note.getLibelle() + " : "
                            + traitee.getReponse());
        }
        return versReponse(traitee);
    }

    private Etudiant etudiantDuCompte(UUID utilisateurId) {
        return etudiantRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new BusinessException(
                        "Ce compte n'est rattaché à aucune fiche étudiant", HttpStatus.FORBIDDEN));
    }

    private GestionResponses.Reclamation versReponse(Reclamation r) {
        Note n = r.getNote();
        Etudiant e = r.getEtudiant();
        Personnel p = r.getEnseignant();
        return new GestionResponses.Reclamation(r.getId(), n.getId(), n.getMatiere().getLibelle(),
                n.getLibelle(), n.getValeur(), e.getId(), (e.getPrenom() + " " + e.getNom()).trim(),
                e.getMatricule(), (p.getPrenom() + " " + p.getNom()).trim(), r.getMotif(), r.getStatut(),
                r.getReponse(), r.getValeurAvant(), r.getValeurApres(), r.getCreatedAt(), r.getTraiteeLe());
    }
}
