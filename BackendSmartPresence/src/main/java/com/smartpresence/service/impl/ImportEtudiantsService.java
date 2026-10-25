package com.smartpresence.service.impl;

import com.smartpresence.dto.request.EtudiantRequest;
import com.smartpresence.dto.request.ImportEtudiantsRequest;
import com.smartpresence.dto.response.ImportEtudiantsResponse;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.service.EtudiantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Inscription d'une liste d'étudiants en une opération.
 *
 * <p>Chaque ligne est inscrite dans sa propre transaction (via {@link EtudiantService}) : une
 * ligne fautive n'annule pas les autres, et le bilan dit précisément laquelle a échoué et
 * pourquoi. Un matricule déjà inscrit est ignoré plutôt que refusé, pour qu'on puisse
 * relancer le même fichier après correction sans créer de doublon.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportEtudiantsService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final EtudiantService etudiantService;
    private final EtudiantRepository etudiantRepository;
    private final ClasseRepository classeRepository;

    public ImportEtudiantsResponse importer(ImportEtudiantsRequest requete) {
        if (!classeRepository.existsById(requete.classeId())) {
            throw new ResourceNotFoundException("Classe", "id", requete.classeId());
        }

        List<ImportEtudiantsResponse.Ligne> lignes = new ArrayList<>();
        Set<String> vus = new HashSet<>();
        int creees = 0;
        int ignorees = 0;
        int erreurs = 0;

        for (int i = 0; i < requete.etudiants().size(); i++) {
            var l = requete.etudiants().get(i);
            int indice = i + 1;
            String matricule = nettoyer(l.matricule()).toUpperCase(Locale.ROOT);
            String nom = nettoyer(l.nom());
            String prenom = nettoyer(l.prenom());
            String email = nettoyer(l.email());

            String probleme = null;
            if (matricule.isEmpty()) probleme = "Matricule manquant";
            else if (nom.isEmpty()) probleme = "Nom manquant";
            else if (prenom.isEmpty()) probleme = "Prénom manquant";
            else if (!email.isEmpty() && !EMAIL.matcher(email).matches()) probleme = "Email invalide";
            else if (matricule.length() > 50) probleme = "Matricule trop long";

            if (probleme != null) {
                lignes.add(new ImportEtudiantsResponse.Ligne(indice, matricule, nom, prenom, "ERREUR", probleme));
                erreurs++;
                continue;
            }
            if (!vus.add(matricule)) {
                lignes.add(new ImportEtudiantsResponse.Ligne(indice, matricule, nom, prenom, "ERREUR",
                        "Matricule en double dans le fichier"));
                erreurs++;
                continue;
            }
            if (etudiantRepository.existsByMatricule(matricule)) {
                lignes.add(new ImportEtudiantsResponse.Ligne(indice, matricule, nom, prenom, "IGNORE",
                        "Déjà inscrit"));
                ignorees++;
                continue;
            }

            if (requete.simulation()) {
                lignes.add(new ImportEtudiantsResponse.Ligne(indice, matricule, nom, prenom, "CREE", "Sera inscrit"));
                creees++;
                continue;
            }

            try {
                etudiantService.create(EtudiantRequest.builder()
                        .matricule(matricule).nom(nom).prenom(prenom)
                        .email(email.isEmpty() ? null : email)
                        .telephone(nettoyer(l.telephone()).isEmpty() ? null : nettoyer(l.telephone()))
                        .dateNaissance(l.dateNaissance())
                        .classeId(requete.classeId()).actif(true).build());
                lignes.add(new ImportEtudiantsResponse.Ligne(indice, matricule, nom, prenom, "CREE", "Inscrit"));
                creees++;
            } catch (BusinessException e) {
                lignes.add(new ImportEtudiantsResponse.Ligne(indice, matricule, nom, prenom, "ERREUR", e.getMessage()));
                erreurs++;
            } catch (RuntimeException e) {
                log.warn("Import étudiant — ligne {} : {}", indice, e.toString());
                lignes.add(new ImportEtudiantsResponse.Ligne(indice, matricule, nom, prenom, "ERREUR",
                        "Enregistrement impossible"));
                erreurs++;
            }
        }

        if (!requete.simulation()) {
            log.info("Import d'étudiants — classe {} : {} inscrit(s), {} ignoré(s), {} erreur(s)",
                    requete.classeId(), creees, ignorees, erreurs);
        }
        return new ImportEtudiantsResponse(requete.simulation(), creees, ignorees, erreurs, lignes);
    }

    private static String nettoyer(String valeur) {
        return valeur == null ? "" : valeur.trim().replaceAll("\\s+", " ");
    }
}
