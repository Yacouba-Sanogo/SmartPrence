package com.smartpresence.utils;

import java.security.SecureRandom;

/**
 * Génération de mots de passe initiaux.
 *
 * <p>Utilisé partout où le système doit créer un accès sans qu'un mot de passe ait été
 * choisi : compte administrateur d'amorçage, comptes étudiants provisionnés en masse.
 * Un mot de passe par défaut connu — même « à changer à la première connexion » — reste
 * une porte ouverte sur toute installation dont l'exploitant a oublié de le faire.</p>
 *
 * <p>L'alphabet écarte les caractères ambigus ({@code 0}/{@code O}, {@code 1}/{@code l}/{@code I}) :
 * ces mots de passe sont recopiés à la main, parfois depuis une console ou un listing
 * papier remis à l'étudiant.</p>
 *
 * @since 0.0.1
 */
public final class GenerateurMotDePasse {

    private static final String ALPHABET =
            "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final int LONGUEUR_PAR_DEFAUT = 12;

    private static final SecureRandom ALEATOIRE = new SecureRandom();

    private GenerateurMotDePasse() {
        // Classe utilitaire : instanciation interdite.
    }

    /** Mot de passe aléatoire de longueur par défaut. */
    public static String generer() {
        return generer(LONGUEUR_PAR_DEFAUT);
    }

    /**
     * Mot de passe aléatoire de la longueur demandée.
     *
     * @param longueur nombre de caractères, au minimum 8
     * @throws IllegalArgumentException si la longueur est inférieure à 8
     */
    public static String generer(int longueur) {
        if (longueur < 8) {
            throw new IllegalArgumentException("Un mot de passe doit compter au moins 8 caractères");
        }
        StringBuilder motDePasse = new StringBuilder(longueur);
        for (int i = 0; i < longueur; i++) {
            motDePasse.append(ALPHABET.charAt(ALEATOIRE.nextInt(ALPHABET.length())));
        }
        return motDePasse.toString();
    }
}
