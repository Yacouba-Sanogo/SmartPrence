package com.smartpresence.service.impl;

import com.smartpresence.dto.request.NumeroCenouRequest;
import com.smartpresence.dto.response.ImportCenouResponse;
import com.smartpresence.dto.response.NumeroCenouResponse;
import com.smartpresence.entity.Classe;
import com.smartpresence.entity.NumeroCenou;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.NumeroCenouMapper;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.NumeroCenouRepository;
import com.smartpresence.service.NumeroCenouService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Référentiel CENOU : tenue de la liste blanche et import depuis un tableur.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NumeroCenouServiceImpl implements NumeroCenouService {

    /**
     * Plafond d'un import.
     *
     * <p>Une promotion entière tient très largement dedans. Au-delà, c'est un fichier
     * qui n'est pas celui qu'on croit : mieux vaut le dire que remplir la base.</p>
     */
    private static final int LIGNES_MAX = 5_000;

    private final NumeroCenouRepository numeroCenouRepository;
    private final ClasseRepository classeRepository;
    private final NumeroCenouMapper numeroCenouMapper;

    @Override
    @Transactional(readOnly = true)
    public List<NumeroCenouResponse> findAll(Boolean utilise) {
        return numeroCenouMapper.toResponseList(utilise == null
                ? numeroCenouRepository.findAll()
                : numeroCenouRepository.findByUtilise(utilise));
    }

    @Override
    @Transactional
    public NumeroCenouResponse create(NumeroCenouRequest request) {
        String numero = normaliser(request.getNumero());

        if (numeroCenouRepository.existsByNumero(numero)) {
            throw new BusinessException(
                    "Le numéro CENOU " + numero + " figure déjà au référentiel", HttpStatus.CONFLICT);
        }

        NumeroCenou entite = new NumeroCenou();
        entite.setNumero(numero);
        entite.setNom(request.getNom());
        entite.setPrenom(request.getPrenom());
        entite.setClasse(classeEventuelle(request.getClasseId()));

        NumeroCenou enregistre = numeroCenouRepository.save(entite);
        log.info("Numéro CENOU ajouté au référentiel : {}", numero);
        return numeroCenouMapper.toResponse(enregistre);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        NumeroCenou entite = numeroCenouRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Numéro CENOU", "id", id));

        if (entite.isUtilise()) {
            throw new BusinessException(
                    "Ce numéro a déjà servi à une inscription : le retirer rouvrirait "
                            + "l'inscription au nom d'un étudiant existant", HttpStatus.CONFLICT);
        }

        numeroCenouRepository.delete(entite);
        log.info("Numéro CENOU retiré du référentiel : {}", entite.getNumero());
    }

    @Override
    @Transactional
    public ImportCenouResponse importer(MultipartFile fichier, Long classeId) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Le fichier est vide", HttpStatus.BAD_REQUEST);
        }

        Classe classe = classeEventuelle(classeId);
        List<String[]> lignes = lire(fichier);

        List<String> rejets = new ArrayList<>();
        Set<String> vusDansLeFichier = new HashSet<>();
        int ajoutes = 0;
        int doublons = 0;

        for (int i = 0; i < lignes.size(); i++) {
            String[] colonnes = lignes.get(i);
            int numeroLigne = i + 1;

            String numero = normaliser(colonnes.length > 0 ? colonnes[0] : null);
            if (numero.isEmpty()) {
                rejets.add("ligne " + numeroLigne + " : numéro absent");
                continue;
            }
            // Un même fichier contient parfois deux fois la même personne ; le
            // signaler comme doublon vaut mieux que de laisser la contrainte
            // d'unicité interrompre tout l'import.
            if (!vusDansLeFichier.add(numero) || numeroCenouRepository.existsByNumero(numero)) {
                doublons++;
                continue;
            }

            NumeroCenou entite = new NumeroCenou();
            entite.setNumero(numero);
            entite.setNom(valeur(colonnes, 1));
            entite.setPrenom(valeur(colonnes, 2));
            entite.setClasse(classe);
            numeroCenouRepository.save(entite);
            ajoutes++;
        }

        log.info("Import CENOU depuis « {} » : {} lue(s), {} ajoutée(s), {} doublon(s), {} rejet(s)",
                fichier.getOriginalFilename(), lignes.size(), ajoutes, doublons, rejets.size());

        return ImportCenouResponse.builder()
                .lignesLues(lignes.size())
                .ajoutes(ajoutes)
                .doublons(doublons)
                .rejets(rejets)
                .build();
    }

    // ------------------------------------------------------------------
    // Lecture du fichier
    // ------------------------------------------------------------------

    /**
     * Extrait les lignes de données, quel que soit le format déposé.
     *
     * <p>L'extension décide : {@code .csv} est lu sans bibliothèque, tout le reste est
     * confié à POI, qui ouvre aussi bien les {@code .xlsx} que les vieux
     * {@code .xls}.</p>
     */
    private List<String[]> lire(MultipartFile fichier) {
        String nom = fichier.getOriginalFilename() == null
                ? "" : fichier.getOriginalFilename().toLowerCase(Locale.ROOT);
        try (InputStream flux = fichier.getInputStream()) {
            return nom.endsWith(".csv") ? lireCsv(flux) : lireTableur(flux);
        } catch (IOException e) {
            throw new BusinessException(
                    "Le fichier n'a pas pu être lu : " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private List<String[]> lireCsv(InputStream flux) throws IOException {
        List<String[]> lignes = new ArrayList<>();
        try (BufferedReader lecteur = new BufferedReader(
                new InputStreamReader(flux, StandardCharsets.UTF_8))) {
            String ligne;
            boolean premiere = true;
            while ((ligne = lecteur.readLine()) != null && lignes.size() < LIGNES_MAX) {
                if (premiere) {
                    // Les fichiers produits sous Windows commencent par une marque
                    // d'ordre d'octets invisible, qui collerait au premier numéro.
                    ligne = ligne.replace("﻿", "");
                }
                if (ligne.isBlank()) {
                    continue;
                }
                String separateur = ligne.contains(";") ? ";" : ",";
                String[] colonnes = ligne.split(separateur, -1);
                if (premiere && estEnTete(colonnes[0])) {
                    premiere = false;
                    continue;
                }
                premiere = false;
                lignes.add(colonnes);
            }
        }
        return lignes;
    }

    private List<String[]> lireTableur(InputStream flux) throws IOException {
        List<String[]> lignes = new ArrayList<>();
        try (Workbook classeur = WorkbookFactory.create(flux)) {
            Sheet feuille = classeur.getSheetAt(0);
            boolean premiere = true;
            for (Row ligne : feuille) {
                if (lignes.size() >= LIGNES_MAX) {
                    break;
                }
                String[] colonnes = {
                        texte(ligne.getCell(0)), texte(ligne.getCell(1)), texte(ligne.getCell(2))};
                if (colonnes[0].isBlank() && colonnes[1].isBlank() && colonnes[2].isBlank()) {
                    continue;
                }
                if (premiere && estEnTete(colonnes[0])) {
                    premiere = false;
                    continue;
                }
                premiere = false;
                lignes.add(colonnes);
            }
        }
        return lignes;
    }

    /**
     * Texte d'une cellule, y compris numérique.
     *
     * <p>Un numéro CENOU saisi sans apostrophe est vu comme un nombre par le tableur.
     * Le lire tel quel donnerait « 1.23456789E8 » : il est donc ramené à sa forme
     * entière, sans notation scientifique ni décimale parasite.</p>
     */
    private String texte(Cell cellule) {
        if (cellule == null) {
            return "";
        }
        if (cellule.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cellule.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        return cellule.toString().trim();
    }

    private boolean estEnTete(String premiereCellule) {
        String valeur = premiereCellule == null ? "" : premiereCellule.trim().toLowerCase(Locale.ROOT);
        return valeur.contains("cenou") || valeur.contains("matricule")
                || valeur.startsWith("numero") || valeur.startsWith("numéro") || valeur.equals("n°");
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------

    /**
     * Forme canonique d'un numéro : sans espaces superflus et en capitales.
     *
     * <p>Une liste importée et une saisie d'étudiant ne s'accordent jamais sur la
     * casse ni sur les espaces. Sans cette normalisation des deux côtés, un numéro
     * pourtant présent au référentiel serait refusé à l'inscription.</p>
     */
    static String normaliser(String numero) {
        return numero == null ? "" : numero.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private String valeur(String[] colonnes, int index) {
        if (colonnes.length <= index || colonnes[index] == null) {
            return null;
        }
        String valeur = colonnes[index].trim();
        return valeur.isEmpty() ? null : valeur;
    }

    private Classe classeEventuelle(Long classeId) {
        if (classeId == null) {
            return null;
        }
        return classeRepository.findById(classeId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe", "id", classeId));
    }
}
