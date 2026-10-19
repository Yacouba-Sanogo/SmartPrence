package com.smartpresence.service;

import com.smartpresence.dto.request.EligibiliteRequest;
import com.smartpresence.dto.response.EligibiliteResponse;
import com.smartpresence.dto.response.RegleConcoursResponse;

import java.util.List;

/** Règles du concours d'entrée à l'ENETP. */
public interface ConcoursService {

    List<RegleConcoursResponse> regles();

    EligibiliteResponse verifier(EligibiliteRequest requete);
}
