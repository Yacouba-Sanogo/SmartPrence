package com.smartpresence.dto.response;

/** Règle d'âge applicable à un type de diplôme. */
public record RegleConcoursResponse(String diplome, String libelle, int ageLimite) {
}
