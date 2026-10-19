package com.smartpresence.dto.response;

import java.util.List;

/** Filière avec son DER et ses options. */
public record FiliereResponse(Long id, String code, String libelle,
                              DepartementResponse departement, List<OptionResponse> options) {

    /** Option (spécialité) d'une filière. */
    public record OptionResponse(Long id, String code, String libelle) {
    }
}
