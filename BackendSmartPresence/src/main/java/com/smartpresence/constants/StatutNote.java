package com.smartpresence.constants;

/**
 * Étape d'une note dans son circuit de validation.
 *
 * <p>{@code BROUILLON} : modifiable par l'enseignant. {@code TRANSMISE} : envoyée à
 * l'administration, verrouillée en attendant sa décision. {@code VALIDEE} : publiée, seule
 * étape que voient les bulletins.</p>
 */
public enum StatutNote { BROUILLON, TRANSMISE, VALIDEE }
