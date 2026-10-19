package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Séances effectuées, cahier de textes, syllabus, évaluations et validation des notes.
 */
@DisplayName("Intégration — enseignement : séances, cahier, évaluations, validation")
class EnseignementEnseignantTest extends IntegrationTestBase {

    // ------------------------------------------------------------------
    // Séance effectuée et bilan
    // ------------------------------------------------------------------

    @Test
    @DisplayName("déclarer une séance passée la termine, de façon idempotente, et alimente le bilan")
    void declarationEtBilan() throws Exception {
        Decor d = preparer("EGN-A");
        String seanceId = creerSeance(d, Instant.now().minus(Duration.ofHours(3)),
                Instant.now().minus(Duration.ofHours(1)));

        JsonNode premiere = appeler(post("/moi/seances/" + seanceId + "/declaration"), d.jeton, 200);
        assertThat(premiere.get("statut").asText()).isEqualTo("TERMINEE");
        String declareeLe = premiere.get("declareeLe").asText();
        assertThat(declareeLe).isNotBlank();

        JsonNode seconde = appeler(post("/moi/seances/" + seanceId + "/declaration"), d.jeton, 200);
        assertThat(seconde.get("declareeLe").asText())
                .as("un second appui ne doit pas déplacer la date de déclaration")
                .isEqualTo(declareeLe);

        JsonNode bilan = appeler(get("/moi/bilan").param("debut", LocalDate.now().minusDays(2).toString())
                .param("fin", LocalDate.now().plusDays(1).toString()), d.jeton, 200);
        assertThat(bilan.get("seancesEffectuees").asInt()).isEqualTo(1);
        assertThat(bilan.get("minutesEnseignees").asLong()).isEqualTo(120);
        assertThat(bilan.get("seancesADeclarer").asInt()).isZero();
    }

    @Test
    @DisplayName("une séance future ne peut pas être déclarée, et celle d'un collègue non plus")
    void declarationRefusee() throws Exception {
        Decor d = preparer("EGN-B");
        String future = creerSeance(d, Instant.now().plus(Duration.ofHours(5)),
                Instant.now().plus(Duration.ofHours(7)));
        appeler(post("/moi/seances/" + future + "/declaration"), d.jeton, 409);

        String passee = creerSeance(d, Instant.now().minus(Duration.ofHours(3)),
                Instant.now().minus(Duration.ofHours(1)));
        appeler(post("/moi/seances/" + passee + "/declaration"), d.jetonCollegue, 403);
    }

    @Test
    @DisplayName("une séance passée non déclarée est comptée à régulariser")
    void seanceADeclarer() throws Exception {
        Decor d = preparer("EGN-C");
        creerSeance(d, Instant.now().minus(Duration.ofHours(3)), Instant.now().minus(Duration.ofHours(1)));

        JsonNode bilan = appeler(get("/moi/bilan").param("debut", LocalDate.now().minusDays(2).toString())
                .param("fin", LocalDate.now().plusDays(1).toString()), d.jeton, 200);
        assertThat(bilan.get("seancesADeclarer").asInt()).isEqualTo(1);
        assertThat(bilan.get("seancesEffectuees").asInt()).isZero();
    }

    @Test
    @DisplayName("l'historique de pointage est ouvert à l'enseignant, pas à l'étudiant")
    void pointagesReserves() throws Exception {
        Decor d = preparer("EGN-D");
        JsonNode journees = appeler(get("/moi/pointages"), d.jeton, 200);
        assertThat(journees.isArray()).isTrue();
        appeler(get("/moi/pointages"), d.jetonEtudiant, 403);
    }

    // ------------------------------------------------------------------
    // Cahier de textes et syllabus
    // ------------------------------------------------------------------

    @Test
    @DisplayName("le cahier de textes lie une séance à un chapitre et en fait avancer le syllabus")
    void cahierEtSyllabus() throws Exception {
        Decor d = preparer("EGN-E");
        String seanceId = creerSeance(d, Instant.now().minus(Duration.ofHours(3)),
                Instant.now().minus(Duration.ofHours(1)));

        assertThat(appeler(get("/moi/seances/" + seanceId + "/cahier"), d.jeton, 200).isNull()).isTrue();

        String chapitre1 = creerChapitre(d, "Introduction");
        creerChapitre(d, "Pointeurs");

        JsonNode page = appeler(put("/moi/seances/" + seanceId + "/cahier").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("contenu", "Définition et premiers exemples",
                        "travailAFaire", "Exercices 1 à 3", "chapitreId", chapitre1,
                        "chapitreTermine", true))), d.jeton, 200);
        assertThat(page.get("chapitreTitre").asText()).isEqualTo("Introduction");

        // Réécrire la page la corrige, elle ne la double pas.
        appeler(put("/moi/seances/" + seanceId + "/cahier").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("contenu", "Version corrigée", "chapitreTermine", false))), d.jeton, 200);
        JsonNode cahier = appeler(get("/moi/classes/" + d.classeId + "/cahier"), d.jeton, 200);
        assertThat(cahier).hasSize(1);
        assertThat(cahier.get(0).get("contenu").asText()).isEqualTo("Version corrigée");

        JsonNode syllabus = appeler(get("/moi/classes/" + d.classeId + "/chapitres"), d.jeton, 200);
        assertThat(syllabus).hasSize(1);
        assertThat(syllabus.get(0).get("total").asInt()).isEqualTo(2);
        assertThat(syllabus.get(0).get("termines").asInt())
                .as("le chapitre marqué terminé par le cahier doit compter dans l'avancement")
                .isEqualTo(1);
        assertThat(syllabus.get(0).get("chapitres").get(0).get("ordre").asInt()).isEqualTo(1);
        assertThat(syllabus.get(0).get("chapitres").get(1).get("ordre").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("un collègue ne touche ni au syllabus ni au cahier d'un autre")
    void syllabusCloisonne() throws Exception {
        Decor d = preparer("EGN-F");
        String chapitre = creerChapitre(d, "Réseaux");

        appeler(put("/moi/chapitres/" + chapitre).contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("titre", "Piraté", "termine", true))), d.jetonCollegue, 403);
        appeler(delete("/moi/chapitres/" + chapitre), d.jetonCollegue, 403);
        // Même classe, autre enseignant : son syllabus est le sien, donc vide — pas celui d'un collègue.
        assertThat(appeler(get("/moi/classes/" + d.classeId + "/chapitres"), d.jetonCollegue, 200)).isEmpty();
        assertThat(appeler(get("/moi/classes/" + d.classeId + "/chapitres"), d.jeton, 200)).hasSize(1);
    }

    // ------------------------------------------------------------------
    // Évaluations et circuit de validation
    // ------------------------------------------------------------------

    @Test
    @DisplayName("les notes se saisissent sur le barème, sont ramenées sur 20, et n'entrent au bulletin qu'une fois validées")
    void evaluationEtValidation() throws Exception {
        Decor d = preparer("EGN-G");
        String evaluationId = planifier(d, 10);

        JsonNode resultats = appeler(put("/moi/evaluations/" + evaluationId + "/resultats")
                .contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("resultats", List.of(
                        Map.of("etudiantId", d.etudiantId, "valeur", 7, "absent", false),
                        Map.of("etudiantId", d.etudiantAbsentId, "absent", true, "motif", "Maladie"))))),
                d.jeton, 200);

        JsonNode noteLigne = ligne(resultats, d.etudiantId);
        assertThat(noteLigne.get("valeurSur20").asDouble()).isEqualTo(14.00);
        assertThat(noteLigne.get("valeurSurBareme").asDouble()).isEqualTo(7.00);
        assertThat(noteLigne.get("statut").asText()).isEqualTo("BROUILLON");
        assertThat(ligne(resultats, d.etudiantAbsentId).get("absent").asBoolean()).isTrue();
        assertThat(resultats.get("evaluation").get("absents").asInt()).isEqualTo(1);

        // Brouillon : l'étudiant ne voit rien.
        assertThat(bulletin(d).get("nombreNotes").asInt()).isZero();

        appeler(post("/moi/evaluations/" + evaluationId + "/transmission"), d.jeton, 200);

        // Transmise : verrouillée pour l'enseignant, toujours invisible de l'étudiant.
        String noteId = noteLigne.get("noteId").asText();
        appeler(put("/moi/notes/" + noteId).contentType(MediaType.APPLICATION_JSON).content(corpsNote(d, "15")),
                d.jeton, 409);
        appeler(delete("/moi/notes/" + noteId), d.jeton, 409);
        assertThat(bulletin(d).get("nombreNotes").asInt()).isZero();

        // L'administration voit la file puis valide.
        JsonNode file = appeler(get("/notes/a-valider").param("classeId", String.valueOf(d.classeId)), jetonAdmin, 200);
        assertThat(file).hasSize(1);
        appeler(post("/notes/validation").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("noteIds", List.of(noteId)))), jetonAdmin, 200);

        JsonNode bulletin = bulletin(d);
        assertThat(bulletin.get("nombreNotes").asInt()).isEqualTo(1);
        assertThat(bulletin.get("moyenneGenerale").asDouble()).isEqualTo(14.00);
    }

    @Test
    @DisplayName("l'administration renvoie une note avec un motif : elle redevient modifiable")
    void renvoiEnBrouillon() throws Exception {
        Decor d = preparer("EGN-H");
        String evaluationId = planifier(d, 20);
        appeler(put("/moi/evaluations/" + evaluationId + "/resultats").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("resultats", List.of(
                        Map.of("etudiantId", d.etudiantId, "valeur", 12, "absent", false))))), d.jeton, 200);
        appeler(post("/moi/evaluations/" + evaluationId + "/transmission"), d.jeton, 200);

        JsonNode file = appeler(get("/notes/a-valider"), jetonAdmin, 200);
        String noteId = file.get(0).get("id").asText();

        appeler(post("/notes/renvoi").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("noteIds", List.of(noteId)))), jetonAdmin, 400);
        appeler(post("/notes/renvoi").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("noteIds", List.of(noteId), "motif", "Vérifier la copie 4"))), jetonAdmin, 200);

        JsonNode resultats = appeler(get("/moi/evaluations/" + evaluationId + "/resultats"), d.jeton, 200);
        JsonNode renvoyee = ligne(resultats, d.etudiantId);
        assertThat(renvoyee.get("statut").asText()).isEqualTo("BROUILLON");
        assertThat(renvoyee.get("motifRejet").asText()).isEqualTo("Vérifier la copie 4");

        appeler(put("/moi/notes/" + noteId).contentType(MediaType.APPLICATION_JSON).content(corpsNote(d, "13")),
                d.jeton, 200);
    }

    @Test
    @DisplayName("garde-fous : note au-dessus du barème, barème figé après saisie, collègue et étudiant refusés")
    void garde_fous() throws Exception {
        Decor d = preparer("EGN-I");
        String evaluationId = planifier(d, 10);

        appeler(put("/moi/evaluations/" + evaluationId + "/resultats").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("resultats", List.of(
                        Map.of("etudiantId", d.etudiantId, "valeur", 11, "absent", false))))), d.jeton, 400);

        appeler(put("/moi/evaluations/" + evaluationId + "/resultats").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("resultats", List.of(
                        Map.of("etudiantId", d.etudiantId, "valeur", 8, "absent", false))))), d.jeton, 200);

        appeler(put("/moi/evaluations/" + evaluationId).contentType(MediaType.APPLICATION_JSON)
                .content(corpsEvaluation(d, 20)), d.jeton, 409);
        appeler(delete("/moi/evaluations/" + evaluationId), d.jeton, 409);

        appeler(get("/moi/evaluations/" + evaluationId + "/resultats"), d.jetonCollegue, 403);
        appeler(put("/moi/evaluations/" + evaluationId + "/resultats").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("resultats", List.of(
                        Map.of("etudiantId", d.etudiantId, "valeur", 1, "absent", false))))), d.jetonCollegue, 403);

        appeler(get("/notes/a-valider"), d.jeton, 403);
        appeler(post("/notes/validation").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("noteIds", List.of("00000000-0000-0000-0000-000000000000")))), d.jeton, 403);
    }

    // ------------------------------------------------------------------
    // Décor
    // ------------------------------------------------------------------

    private record Decor(long classeId, long matiereId, String enseignantId, String etudiantId,
                         String etudiantAbsentId, String jeton, String jetonCollegue, String jetonEtudiant) { }

    private Decor preparer(String cle) throws Exception {
        long promotionId = creer("/promotions", """
                {"code":"PROMO-%s","libelle":"Promotion %s","anneeAcademique":2026}
                """.formatted(cle, cle)).get("id").asLong();
        long classeId = creer("/classes", """
                {"code":"%s","libelle":"Classe %s","promotionId":%d}
                """.formatted(cle, cle, promotionId)).get("id").asLong();
        String enseignantId = creer("/personnels", """
                {"matricule":"PROF-%s-1","nom":"Diallo","prenom":"Awa","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();
        String collegueId = creer("/personnels", """
                {"matricule":"PROF-%s-2","nom":"Sow","prenom":"Modibo","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();

        mockMvc.perform(avecJeton(put("/classes/" + classeId), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","libelle":"Classe %s","promotionId":%d,"enseignantIds":["%s","%s"]}
                                """.formatted(cle, cle, promotionId, enseignantId, collegueId)))
                .andExpect(status().isOk());

        long matiereId = creer("/matieres", """
                {"code":"MAT-%s","libelle":"Matiere %s","credits":3}
                """.formatted(cle, cle)).get("id").asLong();
        String etudiantId = creer("/etudiants", """
                {"matricule":"%s-ET1","nom":"Keita","prenom":"Sira","classeId":%d,"actif":true}
                """.formatted(cle, classeId)).get("id").asText();
        String absentId = creer("/etudiants", """
                {"matricule":"%s-ET2","nom":"Coulibaly","prenom":"Bakary","classeId":%d,"actif":true}
                """.formatted(cle, classeId)).get("id").asText();

        return new Decor(classeId, matiereId, enseignantId, etudiantId, absentId,
                jetonDe("/personnels/" + enseignantId + "/compte"),
                jetonDe("/personnels/" + collegueId + "/compte"),
                jetonDe("/etudiants/" + etudiantId + "/compte"));
    }

    private String creerSeance(Decor d, Instant debut, Instant fin) throws Exception {
        return creer("/seances", """
                {"classeId":%d,"matiereId":%d,"enseignantId":"%s","debut":"%s","fin":"%s"}
                """.formatted(d.classeId, d.matiereId, d.enseignantId, debut, fin)).get("id").asText();
    }

    private String creerChapitre(Decor d, String titre) throws Exception {
        return appeler(post("/moi/chapitres").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("classeId", d.classeId, "matiereId", d.matiereId, "titre", titre))),
                d.jeton, 201).get("id").asText();
    }

    private String corpsEvaluation(Decor d, int bareme) throws Exception {
        return enJson(Map.of("classeId", d.classeId, "matiereId", d.matiereId, "type", "DEVOIR",
                "libelle", "Devoir 1", "dateEvaluation", LocalDate.now().toString(),
                "bareme", bareme, "coefficient", 1, "periode", "SEMESTRE_1"));
    }

    private String planifier(Decor d, int bareme) throws Exception {
        return appeler(post("/moi/evaluations").contentType(MediaType.APPLICATION_JSON)
                .content(corpsEvaluation(d, bareme)), d.jeton, 201).get("id").asText();
    }

    private String corpsNote(Decor d, String valeur) {
        return """
                {"etudiantId":"%s","matiereId":%d,"valeur":%s,"coefficient":1,
                 "libelle":"Devoir 1","dateEvaluation":"%s","type":"DEVOIR"}
                """.formatted(d.etudiantId, d.matiereId, valeur, LocalDate.now());
    }

    private JsonNode ligne(JsonNode resultats, String etudiantId) {
        for (JsonNode ligne : resultats.get("lignes")) {
            if (ligne.get("etudiantId").asText().equals(etudiantId)) {
                return ligne;
            }
        }
        throw new AssertionError("étudiant absent des résultats : " + etudiantId);
    }

    private JsonNode bulletin(Decor d) throws Exception {
        return appeler(get("/moi/bulletin"), d.jetonEtudiant, 200);
    }

    private JsonNode appeler(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requete,
                             String jeton, int statutAttendu) throws Exception {
        var resultat = mockMvc.perform(avecJeton(requete, jeton)).andReturn();
        assertThat(resultat.getResponse().getStatus())
                .as("réponse : %s", resultat.getResponse().getContentAsString())
                .isEqualTo(statutAttendu);
        String corps = resultat.getResponse().getContentAsString();
        JsonNode data = corps.isBlank() ? null : objectMapper.readTree(corps).get("data");
        // L'enveloppe omet « data » quand il est nul.
        return data == null ? com.fasterxml.jackson.databind.node.NullNode.instance : data;
    }

    private String jetonDe(String cheminCompte) throws Exception {
        JsonNode compte = creer(cheminCompte, null);
        return seConnecter(compte.get("email").asText(), compte.get("motDePasseInitial").asText());
    }

    private JsonNode creer(String chemin, String corps) throws Exception {
        var requete = avecJeton(post(chemin), jetonAdmin);
        if (corps != null) {
            requete = requete.contentType(MediaType.APPLICATION_JSON).content(corps);
        }
        return donnees(mockMvc.perform(requete).andExpect(status().isCreated()).andReturn());
    }
}
