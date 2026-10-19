package com.smartpresence.dto.response;

import com.smartpresence.constants.StatutDemande;
import com.smartpresence.constants.StatutReclamation;
import com.smartpresence.constants.TypeConge;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class GestionResponses {

    private GestionResponses() { }

    /** {@code seancesConcernees} : séances non annulées de l'enseignant sur la période. */
    public record Conge(
            UUID id, UUID enseignantId, String enseignantNom, TypeConge type,
            LocalDate debut, LocalDate fin, String motif, StatutDemande statut,
            String reponse, Instant traiteeLe, int seancesConcernees, Instant creeLe) { }

    public record Support(
            UUID id, Long classeId, String classeCode, Long matiereId, String matiereLibelle,
            String enseignantNom, String titre, String description, String nomFichier,
            String typeMime, long taille, Instant publieLe) { }

    public record Reclamation(
            UUID id, UUID noteId, String matiereLibelle, String libelleNote,
            BigDecimal valeurActuelle, UUID etudiantId, String etudiantNom, String etudiantMatricule,
            String enseignantNom, String motif, StatutReclamation statut, String reponse,
            BigDecimal valeurAvant, BigDecimal valeurApres, Instant creeLe, Instant traiteeLe) { }
}
