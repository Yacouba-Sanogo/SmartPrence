package com.smartpresence.dto.response;

import java.util.List;

/** Option choisie par l'étudiant et options que sa filière lui propose. */
public record ChoixOptionResponse(String filiere, FiliereResponse.OptionResponse choisie,
                                  List<FiliereResponse.OptionResponse> disponibles) {
}
