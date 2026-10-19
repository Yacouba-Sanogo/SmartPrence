package com.smartpresence.service.impl;

import com.smartpresence.constants.DiplomeCandidat;
import com.smartpresence.dto.request.EligibiliteRequest;
import com.smartpresence.dto.response.EligibiliteResponse;
import com.smartpresence.dto.response.RegleConcoursResponse;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.service.ConcoursService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Éligibilité au concours : l'âge, calculé à la date de référence, ne doit pas dépasser
 * la limite fixée par le diplôme présenté.
 */
@Service
public class ConcoursServiceImpl implements ConcoursService {

    private static final List<String> PIECES_COMMUNES = List.of(
            "Demande manuscrite timbrée à 200 F CFA, précisant la filière choisie",
            "Copie certifiée conforme du baccalauréat ou de l'attestation du diplôme",
            "Extrait d'acte de naissance ou jugement supplétif",
            "Certificat de nationalité");

    private static final String PIECE_PROFESSIONNEL =
            "Autorisation de la Fonction publique (formation continue)";

    @Override
    public List<RegleConcoursResponse> regles() {
        return Arrays.stream(DiplomeCandidat.values())
                .map(d -> new RegleConcoursResponse(d.name(), d.getLibelle(), d.getAgeLimite()))
                .toList();
    }

    @Override
    public EligibiliteResponse verifier(EligibiliteRequest requete) {
        LocalDate reference = requete.dateReference() != null ? requete.dateReference() : LocalDate.now();
        if (!requete.dateNaissance().isBefore(reference)) {
            throw new BusinessException("La date de naissance doit précéder la date de référence");
        }

        int age = Period.between(requete.dateNaissance(), reference).getYears();
        DiplomeCandidat diplome = requete.diplome();
        boolean eligible = age <= diplome.getAgeLimite();

        List<String> pieces = new ArrayList<>(PIECES_COMMUNES);
        if (diplome == DiplomeCandidat.PROFESSIONNEL) {
            pieces.add(PIECE_PROFESSIONNEL);
        }

        String message = eligible
                ? "Vous pouvez vous présenter : à " + age + " ans, vous êtes sous la limite de "
                        + diplome.getAgeLimite() + " ans."
                : "Vous ne pouvez pas vous présenter : à " + age + " ans, vous dépassez la limite de "
                        + diplome.getAgeLimite() + " ans pour ce diplôme.";

        return new EligibiliteResponse(eligible, age, diplome.getAgeLimite(),
                diplome.getLibelle(), message, eligible ? pieces : List.of());
    }
}
