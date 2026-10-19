package com.smartpresence.service;

import com.smartpresence.dto.request.ReferentielRequests;
import com.smartpresence.dto.response.DepartementResponse;
import com.smartpresence.dto.response.FiliereResponse;

import java.util.List;

/** Référentiel des départements (DER), filières et options de l'ENETP. */
public interface ReferentielService {

    List<DepartementResponse> departements();

    List<FiliereResponse> filieres();

    DepartementResponse creerDepartement(ReferentielRequests.Departement requete);

    FiliereResponse creerFiliere(ReferentielRequests.Filiere requete);

    FiliereResponse ajouterOption(ReferentielRequests.Option requete);

    void supprimerOption(Long id);
}
