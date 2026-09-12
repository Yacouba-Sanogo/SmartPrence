package com.smartpresence.constants;

/**
 * Noms physiques des tables de la base de données SmartPresence.
 *
 * <p>Centralisation des noms de tables en {@code snake_case} pluriel, afin d'éviter la
 * dispersion de chaînes magiques dans les annotations JPA et de garantir la cohérence
 * avec le modèle relationnel v2 (cf. {@code SmartPresence_CONTEXT.md} §9).</p>
 *
 * <p>Utilisation type :</p>
 * <pre>{@code
 * @Entity
 * @Table(name = TableNames.UTILISATEURS,
 *        uniqueConstraints = @UniqueConstraint(columnNames = TableNames.COL_EMAIL))
 * }</pre>
 *
 * @since 0.0.1
 */
public final class TableNames {

    private TableNames() {
        // Classe utilitaire : instanciation interdite.
    }

    // ---------------- Tables métier ----------------
    public static final String UTILISATEURS = "utilisateurs";
    public static final String ROLES = "roles";
    public static final String ETUDIANTS = "etudiants";
    public static final String ENSEIGNANTS = "enseignants";
    public static final String PERSONNELS = "personnels";
    public static final String MATIERES = "matieres";
    public static final String SEANCES = "seances";
    public static final String PRESENCES_PERSONNEL = "presences_personnel";
    public static final String JUSTIFICATIONS_ABSENCE = "justifications_absence";
    public static final String NOTIFICATIONS = "notifications";
    public static final String PARAMETRES_ETABLISSEMENT = "parametres_etablissement";
    public static final String CLASSES = "classes";
    public static final String PROMOTIONS = "promotions";
    public static final String SALLES = "salles";
    public static final String PRESENCES = "presences";
    public static final String NOTES = "notes";
    public static final String DEVICES = "devices";
    public static final String HISTORIQUES_SYNCHRONISATION = "historiques_synchronisation";
    public static final String SIGNALEMENTS_PRESENCE = "signalements_presence";
    public static final String NUMEROS_CENOU = "numeros_cenou";
    public static final String DEMANDES_ENROLEMENT = "demandes_enrolement";
    public static final String UNITES_ENSEIGNEMENT = "unites_enseignement";

    // ---------------- Tables de jointure ----------------
    public static final String JOIN_UTILISATEURS_ROLES = "utilisateurs_roles";
    public static final String JOIN_CLASSES_ENSEIGNANTS = "classes_enseignants";

    // ---------------- Noms de colonnes usuels (pour contraintes uniques) ----------------
    public static final String COL_EMAIL = "email";
    public static final String COL_MATRICULE = "matricule";
    public static final String COL_NUMERO = "numero";
    public static final String COL_BIOMETRIC_ID = "biometric_id";
    public static final String COL_API_KEY_HASH = "api_key_hash";
    public static final String COL_ADRESSE_MAC = "adresse_mac";
    public static final String COL_CODE = "code";

    // ---------------- Colonnes de la contrainte d'idempotence des présences ----------------
    public static final String COL_DEVICE_ID = "device_id";
    public static final String COL_ETUDIANT_ID = "etudiant_id";
    public static final String COL_MATIERE_ID = "matiere_id";
    public static final String COL_PERIODE = "periode";
    public static final String COL_SEMESTRE = "semestre";
    public static final String COL_DATE_PRESENCE = "date_presence";
    public static final String COL_HEURE_PRESENCE = "heure_presence";

    // ---------------- Colonnes de la contrainte d'idempotence des pointages personnel ----------------
    public static final String COL_PERSONNEL_ID = "personnel_id";
    public static final String COL_DATE_POINTAGE = "date_pointage";
    public static final String COL_HEURE_POINTAGE = "heure_pointage";

    /**
     * Vocation de l'appareil ESP32.
     * <p>Le nom {@code usage} seul est un mot réservé MySQL : la colonne est donc
     * suffixée pour rester utilisable sans échappement.</p>
     */
    public static final String COL_USAGE_DEVICE = "usage_device";

}
