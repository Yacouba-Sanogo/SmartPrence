package com.smartpresence.constants;

/**
 * Diplôme présenté par un candidat au concours d'entrée, qui fixe sa limite d'âge.
 */
public enum DiplomeCandidat {

    /** Candidat au baccalauréat ou titulaire. */
    BACCALAUREAT("Baccalauréat", 22),

    /** Titulaire d'une licence. */
    LICENCE("Licence", 25),

    /** DUT, maîtrise ou autre diplôme universitaire. */
    AUTRE_DIPLOME("DUT, maîtrise ou autre diplôme", 30),

    /** Professionnel en formation continue : exige l'autorisation de la Fonction publique. */
    PROFESSIONNEL("Professionnel (formation continue)", 30);

    private final String libelle;
    private final int ageLimite;

    DiplomeCandidat(String libelle, int ageLimite) {
        this.libelle = libelle;
        this.ageLimite = ageLimite;
    }

    public String getLibelle() {
        return libelle;
    }

    public int getAgeLimite() {
        return ageLimite;
    }
}
