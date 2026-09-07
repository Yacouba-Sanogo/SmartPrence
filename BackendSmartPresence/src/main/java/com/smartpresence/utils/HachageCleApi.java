package com.smartpresence.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Empreinte déterministe d'une clé d'API d'appareil ESP32.
 *
 * <h2>Pourquoi SHA-256 et non BCrypt</h2>
 * <p>BCrypt est conçu pour les <b>mots de passe humains</b> : à faible entropie, ils
 * exigent un hachage lent et salé pour résister aux attaques par dictionnaire. Une clé
 * d'API est un secret <b>aléatoire à forte entropie</b> : le dictionnaire n'a aucune
 * prise sur elle, et le salage devient un handicap.</p>
 *
 * <p>Le sel aléatoire de BCrypt produit en effet une empreinte différente à chaque appel
 * pour une même clé. Deux conséquences, toutes deux constatées sur ce projet :</p>
 * <ul>
 *   <li>le contrôle d'unicité était <b>inopérant</b> — deux appareils pouvaient porter la
 *       même clé, rendant impossible la révocation de l'un sans l'autre et
 *       l'attribution des pointages non déterministe ;</li>
 *   <li>l'authentification devait <b>parcourir tout le parc</b> et tester BCrypt sur
 *       chaque appareil, à chaque requête — un coût qui croît linéairement avec le
 *       nombre de lecteurs déployés.</li>
 * </ul>
 *
 * <p>Une empreinte déterministe restaure la contrainte d'unicité en base et ramène
 * l'authentification à une seule lecture indexée. C'est la pratique courante pour les
 * jetons d'API.</p>
 *
 * @since 0.0.1
 */
public final class HachageCleApi {

    /** Longueur de l'empreinte hexadécimale SHA-256. */
    public static final int LONGUEUR_EMPREINTE = 64;

    private static final String ALGORITHME = "SHA-256";

    private HachageCleApi() {
        // Classe utilitaire : instanciation interdite.
    }

    /**
     * Calcule l'empreinte hexadécimale d'une clé d'API.
     *
     * @param cleEnClair clé transmise par l'appareil ou saisie à l'enregistrement
     * @return empreinte SHA-256 sur 64 caractères hexadécimaux
     * @throws IllegalArgumentException si la clé est nulle ou vide
     */
    public static String empreinte(String cleEnClair) {
        if (cleEnClair == null || cleEnClair.isBlank()) {
            throw new IllegalArgumentException("La clé d'API ne peut pas être vide");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHME);
            byte[] condensat = digest.digest(cleEnClair.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(condensat);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est requis par la spécification de la plateforme Java.
            throw new IllegalStateException("Algorithme " + ALGORITHME + " indisponible", e);
        }
    }
}
