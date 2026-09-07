package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrat consommé par l'application mobile enseignant.
 *
 * <p>Ces tests ne vérifient pas une règle métier mais la <b>forme</b> des réponses :
 * chaque champ listé ici est lu nommément par un parseur Dart. Un renommage côté
 * serveur casserait l'écran mobile silencieusement — le client, faute de mieux,
 * retomberait sur une valeur par défaut plutôt que d'échouer.</p>
 */
@DisplayName("Contrat — mobile enseignant")
class ContratMobileEnseignantTest extends IntegrationTestBase {

    @Test
    @DisplayName("GET /moi/seances expose les champs lus par l'écran Journée")
    void contratDesSeances() throws Exception {
        Contexte c = preparer("MOB-A");

        JsonNode seances = donnees(mockMvc.perform(
                        avecJeton(get("/moi/seances").param("date", LocalDate.now().toString()),
                                c.jetonEnseignant))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(seances.isArray()).isTrue();
        assertThat(seances).hasSize(1);
        assertQueTousPresents(seances.get(0),
                "id", "matiereLibelle", "classeCode", "debut", "fin", "statut");
    }

    @Test
    @DisplayName("GET /moi/classes expose la promotion imbriquée et l'effectif")
    void contratDesClasses() throws Exception {
        Contexte c = preparer("MOB-B");

        JsonNode classes = donnees(mockMvc.perform(
                        avecJeton(get("/moi/classes"), c.jetonEnseignant))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(classes.isArray()).isTrue();
        JsonNode classe = classes.get(0);
        assertQueTousPresents(classe, "id", "code", "libelle", "nombreEtudiants");

        // Le parseur Dart descend dans `promotion.libelle` : l'objet doit être imbriqué,
        // et non aplati en `promotionLibelle`.
        assertThat(classe.get("promotion")).isNotNull();
        assertThat(classe.get("promotion").get("libelle").asText()).isNotBlank();
        assertThat(classe.get("nombreEtudiants").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("GET /moi/seances/{id}/feuille distingue les absents non enrôlés")
    void contratDeLaFeuille() throws Exception {
        Contexte c = preparer("MOB-C");

        JsonNode feuille = donnees(mockMvc.perform(
                        avecJeton(get("/moi/seances/" + c.seanceId + "/feuille"),
                                c.jetonEnseignant))
                .andExpect(status().isOk())
                .andReturn());

        assertQueTousPresents(feuille,
                "seanceId", "matiereLibelle", "classeCode", "debut",
                "effectif", "presents", "retards", "absents", "absentsNonEnroles", "lignes");

        assertThat(feuille.get("effectif").asInt()).isEqualTo(2);

        // Aucun des deux étudiants n'est enrôlé : les deux absences sont donc
        // ininterprétables, et l'écran doit pouvoir le dire.
        assertThat(feuille.get("absents").asInt()).isEqualTo(2);
        assertThat(feuille.get("absentsNonEnroles").asInt())
                .as("sans ce compteur, le mobile presenterait deux absences comme etablies")
                .isEqualTo(2);

        JsonNode ligne = feuille.get("lignes").get(0);
        assertQueTousPresents(ligne, "etudiantId", "matricule", "nom", "prenom",
                "enrole", "statut");
        assertThat(ligne.get("enrole").asBoolean()).isFalse();
        assertThat(ligne.get("heure").isNull()).isTrue();
        assertThat(ligne.get("source").isNull()).isTrue();
    }

    @Test
    @DisplayName("GET /moi/classes/{id}/etudiants expose les champs lus par l'écran Effectif")
    void contratDeLEffectif() throws Exception {
        Contexte c = preparer("MOB-D");

        JsonNode etudiants = donnees(mockMvc.perform(
                        avecJeton(get("/moi/classes/" + c.classeId + "/etudiants"), c.jetonEnseignant))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(etudiants.isArray()).isTrue();
        assertThat(etudiants).hasSize(2);
        assertQueTousPresents(etudiants.get(0),
                "id", "matricule", "nom", "prenom", "enrole", "actif");

        // Le tri est fait par le serveur : le client mobile n'en refait pas.
        assertThat(etudiants.get(0).get("nom").asText())
                .isLessThanOrEqualTo(etudiants.get(1).get("nom").asText());
    }

    @Test
    @DisplayName("GET /moi/signalements expose le suivi lu par l'écran Signalements")
    void contratDuSuivi() throws Exception {
        Contexte c = preparer("MOB-E");

        mockMvc.perform(avecJeton(post("/moi/seances/" + c.seanceId + "/signalements"),
                        c.jetonEnseignant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"LECTEUR_DEFAILLANT",
                                 "description":"Lecteur eteint pendant toute la seance."}
                                """))
                .andExpect(status().isCreated());

        JsonNode signalements = donnees(mockMvc.perform(
                        avecJeton(get("/moi/signalements"), c.jetonEnseignant))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(signalements).hasSize(1);
        assertQueTousPresents(signalements.get(0),
                "id", "seanceId", "matiereLibelle", "classeCode", "seanceDebut",
                "type", "description", "statut", "createdAt");
        assertThat(signalements.get(0).get("statut").asText()).isEqualTo("EN_ATTENTE");
    }

    // ------------------------------------------------------------------

    /**
     * Vérifie qu'aucun champ n'est absent ou nul.
     *
     * <p>Jackson omet silencieusement un champ inconnu et Dart retombe sur sa valeur par
     * défaut : sans cette assertion, un renommage passerait pour un écran vide.</p>
     */
    private void assertQueTousPresents(JsonNode noeud, String... champs) {
        List<String> manquants = new ArrayList<>();
        for (String champ : champs) {
            JsonNode valeur = noeud.get(champ);
            if (valeur == null || valeur.isNull()) {
                manquants.add(champ);
            }
        }
        assertThat(manquants)
                .as("champs attendus par le parseur Dart, absents de %s", noeud)
                .isEmpty();
    }

    private record Contexte(long classeId, String seanceId, String jetonEnseignant) {}

    /** Monte une classe de deux étudiants non enrôlés et une séance du jour. */
    private Contexte preparer(String cle) throws Exception {
        long promotionId = creer("/promotions", """
                {"code":"PROMO-%s","libelle":"Licence 3 %s","anneeAcademique":2026}
                """.formatted(cle, cle)).get("id").asLong();

        long classeId = creer("/classes", """
                {"code":"%s","libelle":"Classe %s","promotionId":%d}
                """.formatted(cle, cle, promotionId)).get("id").asLong();

        String enseignantId = creer("/personnels", """
                {"matricule":"PROF-%s","nom":"Traore","prenom":"Awa","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();

        mockMvc.perform(avecJeton(put("/classes/" + classeId), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","libelle":"Classe %s","promotionId":%d,"enseignantIds":["%s"]}
                                """.formatted(cle, cle, promotionId, enseignantId)))
                .andExpect(status().isOk());

        for (int i = 1; i <= 2; i++) {
            creer("/etudiants", """
                    {"matricule":"%s-ET%d","nom":"Sanogo","prenom":"Etudiant%d",
                     "classeId":%d,"actif":true}
                    """.formatted(cle, i, i, classeId));
        }

        long matiereId = creer("/matieres", """
                {"code":"MAT-%s","libelle":"Algorithmique %s","credits":4}
                """.formatted(cle, cle)).get("id").asLong();

        ZoneId zone = ZoneId.systemDefault();
        Instant debut = LocalDate.now().atTime(8, 0).atZone(zone).toInstant();
        Instant fin = LocalDate.now().atTime(10, 0).atZone(zone).toInstant();
        String seanceId = creer("/seances", """
                {"classeId":%d,"matiereId":%d,"enseignantId":"%s","debut":"%s","fin":"%s"}
                """.formatted(classeId, matiereId, enseignantId, debut, fin)).get("id").asText();

        JsonNode compte = creer("/personnels/" + enseignantId + "/compte", null);
        String jeton = seConnecter(
                compte.get("email").asText(), compte.get("motDePasseInitial").asText());

        return new Contexte(classeId, seanceId, jeton);
    }

    private JsonNode creer(String chemin, String corps) throws Exception {
        var requete = avecJeton(post(chemin), jetonAdmin);
        if (corps != null) {
            requete = requete.contentType(MediaType.APPLICATION_JSON).content(corps);
        }
        MvcResult resultat = mockMvc.perform(requete).andExpect(status().isCreated()).andReturn();
        return donnees(resultat);
    }
}
