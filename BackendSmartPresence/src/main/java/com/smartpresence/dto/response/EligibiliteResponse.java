package com.smartpresence.dto.response;

import java.util.List;

/** Verdict d'éligibilité au concours d'entrée, avec les pièces à réunir. */
public record EligibiliteResponse(boolean eligible, int age, int ageLimite,
                                  String diplome, String message,
                                  List<String> piecesAFournir) {
}
