package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Intégration — relevé de notes au format LMD.
 *
 * <p>Un relevé faux est pire qu'un relevé absent : il se croit. Tous les chiffres de
 * ces tests sont donc calculés à la main dans les commentaires, et choisis pour que
 * la règle qu'ils vérifient soit la <b>seule</b> à les produire — une pondération
 * 50/50 ou une moyenne des moyennes donnerait ailleurs.</p>
 *
 * <p>La maquette est la même partout : deux UE, quatre ECUE, trente crédits.</p>
 */
@DisplayName("Intégration — relevé de notes LMD")
class ReleveLmdTest extends IntegrationTestBase {

    @Test
    @DisplayName("l'ECUE combine devoir et examen à 40 / 60")
    void moyenneEcuePonderee() throws Exception {
        Decor d = preparer("LMD-A");

        // Devoir 12, examen 15 → 0,4×12 + 0,6×15 = 13,80.
        // Une moyenne simple donnerait 13,50 ; du 50/50, 13,50 aussi.
        noter(d, d.ecueA, "12.00", "DEVOIR");
        noter(d, d.ecueA, "15.00", "EXAMEN");

        JsonNode ecue = ecue(releve(d), 0, 0);
        assertThat(ecue.get("noteDevoir").asDouble()).isEqualTo(12.00);
        assertThat(ecue.get("noteExamen").asDouble()).isEqualTo(15.00);
        assertThat(ecue.get("moyenne").asDouble()).isEqualTo(13.80);
    }

    @Test
    @DisplayName("sans examen, le devoir fait la moyenne à lui seul")
    void examenAbsentNePenalisePas() throws Exception {
        Decor d = preparer("LMD-B");

        // 12 de devoir et pas encore d'examen : la moyenne est 12, et non 4,80.
        // Appliquer la pondération à une composante manquante afficherait un échec
        // à un étudiant dont l'examen n'a simplement pas eu lieu.
        noter(d, d.ecueA, "12.00", "DEVOIR");

        JsonNode ecue = ecue(releve(d), 0, 0);
        assertThat(ecue.get("moyenne").asDouble()).isEqualTo(12.00);
        assertThat(ecue.get("noteExamen").isNull()).isTrue();
    }

    @Test
    @DisplayName("la moyenne d'UE pondère les ECUE par leurs crédits, la générale par ceux des UE")
    void ponderationParLesCredits() throws Exception {
        Decor d = preparer("LMD-C");

        // UE1 (12 cr) : ECUE A 6 cr → 13,80 ; ECUE B 6 cr → 8,80.
        //   (13,80×6 + 8,80×6) / 12 = 11,30
        noter(d, d.ecueA, "12.00", "DEVOIR");
        noter(d, d.ecueA, "15.00", "EXAMEN");
        noter(d, d.ecueB, "10.00", "DEVOIR");
        noter(d, d.ecueB, "8.00", "EXAMEN");

        // UE2 (18 cr) : ECUE C 9 cr → 15,20 ; ECUE D 9 cr → 12,20.
        //   (15,20×9 + 12,20×9) / 18 = 13,70
        noter(d, d.ecueC, "14.00", "DEVOIR");
        noter(d, d.ecueC, "16.00", "EXAMEN");
        noter(d, d.ecueD, "11.00", "DEVOIR");
        noter(d, d.ecueD, "13.00", "EXAMEN");

        // Générale : (11,30×12 + 13,70×18) / 30 = 12,74.
        // La moyenne des deux UE donnerait 12,50 — d'où l'écart de crédits choisi.
        JsonNode releve = releve(d);
        assertThat(releve.get("unites").get(0).get("moyenne").asDouble()).isEqualTo(11.30);
        assertThat(releve.get("unites").get(1).get("moyenne").asDouble()).isEqualTo(13.70);
        assertThat(releve.get("moyenneGenerale").asDouble()).isEqualTo(12.74);

        assertThat(releve.get("decision").asText()).isEqualTo("VALIDE");
        assertThat(releve.get("creditsAcquis").asInt()).isEqualTo(30);
        assertThat(releve.get("creditsRequis").asInt()).isEqualTo(30);
        assertThat(releve.get("compensationAppliquee").asBoolean())
                .as("les deux UE sont au-dessus de la barre : rien à compenser")
                .isFalse();
    }

    @Test
    @DisplayName("une UE sous la barre est acquise par la compensation du semestre")
    void compensationEntreUnites() throws Exception {
        Decor d = preparer("LMD-D");

        // UE1 → 8,10 : sous la barre.
        noter(d, d.ecueA, "8.00", "DEVOIR");
        noter(d, d.ecueA, "9.00", "EXAMEN");
        noter(d, d.ecueB, "7.00", "DEVOIR");
        noter(d, d.ecueB, "8.00", "EXAMEN");

        // UE2 → 13,60. Générale : (8,10×12 + 13,60×18) / 30 = 11,40.
        noter(d, d.ecueC, "14.00", "DEVOIR");
        noter(d, d.ecueC, "15.00", "EXAMEN");
        noter(d, d.ecueD, "12.00", "DEVOIR");
        noter(d, d.ecueD, "13.00", "EXAMEN");

        JsonNode releve = releve(d);
        assertThat(releve.get("moyenneGenerale").asDouble()).isEqualTo(11.40);

        JsonNode ue1 = releve.get("unites").get(0);
        assertThat(ue1.get("moyenne").asDouble()).isEqualTo(8.10);
        assertThat(ue1.get("acquise").asBoolean())
                .as("le semestre est à la barre : l'UE faible est rattrapée")
                .isTrue();
        assertThat(ue1.get("acquiseParCompensation").asBoolean()).isTrue();
        assertThat(ue1.get("creditsAcquis").asInt()).isEqualTo(12);

        assertThat(releve.get("creditsAcquis").asInt()).isEqualTo(30);
        assertThat(releve.get("decision").asText()).isEqualTo("VALIDE");
        assertThat(releve.get("compensationAppliquee").asBoolean())
                .as("l'étudiant doit savoir que ses crédits viennent d'une compensation")
                .isTrue();
    }

    @Test
    @DisplayName("sous la barre, seules les UE acquises apportent leurs crédits")
    void semestreNonValideDonneDesCreditsPartiels() throws Exception {
        Decor d = preparer("LMD-E");

        // UE1 → 5,50.
        noter(d, d.ecueA, "5.00", "DEVOIR");
        noter(d, d.ecueA, "6.00", "EXAMEN");
        noter(d, d.ecueB, "6.00", "DEVOIR");
        noter(d, d.ecueB, "5.00", "EXAMEN");

        // UE2 → 10,80. Générale : (5,50×12 + 10,80×18) / 30 = 8,68.
        noter(d, d.ecueC, "11.00", "DEVOIR");
        noter(d, d.ecueC, "12.00", "EXAMEN");
        noter(d, d.ecueD, "10.00", "DEVOIR");
        noter(d, d.ecueD, "10.00", "EXAMEN");

        JsonNode releve = releve(d);
        assertThat(releve.get("moyenneGenerale").asDouble()).isEqualTo(8.68);
        assertThat(releve.get("decision").asText()).isEqualTo("NON_VALIDE");

        assertThat(releve.get("unites").get(0).get("acquise").asBoolean()).isFalse();
        assertThat(releve.get("unites").get(1).get("acquise").asBoolean())
                .as("10,80 se passe de compensation")
                .isTrue();
        assertThat(releve.get("unites").get(1).get("acquiseParCompensation").asBoolean()).isFalse();

        assertThat(releve.get("creditsAcquis").asInt()).isEqualTo(18);
        assertThat(releve.get("creditsRequis").asInt()).isEqualTo(30);
    }

    @Test
    @DisplayName("sans note, le semestre est en attente — pas en échec")
    void sansNoteLeSemestreEstEnAttente() throws Exception {
        Decor d = preparer("LMD-F");

        JsonNode releve = releve(d);
        assertThat(releve.get("decision").asText())
                .as("annoncer un échec avant la saisie des notes serait faux")
                .isEqualTo("EN_ATTENTE");
        assertThat(releve.get("moyenneGenerale").isNull()).isTrue();
        assertThat(releve.get("creditsAcquis").asInt()).isZero();
        assertThat(releve.get("creditsRequis").asInt()).isEqualTo(30);
        assertThat(releve.get("semestresDisponibles"))
                .as("les onglets à proposer, et eux seuls")
                .hasSize(1);
        assertThat(releve.get("semestreLibelle").asText()).isEqualTo("S1");
    }

    @Test
    @DisplayName("le relevé d'un autre étudiant reste hors de portée")
    void cloisonnement() throws Exception {
        Decor d = preparer("LMD-G");

        // L'adresse de l'étudiant ne prend aucun identifiant : le sujet vient du jeton.
        mockMvc.perform(avecJeton(get("/etudiants/" + d.etudiantId + "/releve"), d.jetonEtudiant))
                .andExpect(status().isForbidden());

        // La scolarité, elle, consulte le relevé de qui elle veut.
        mockMvc.perform(avecJeton(get("/etudiants/" + d.etudiantId + "/releve"), jetonAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("une UE ne se supprime pas tant qu'elle regroupe des matières")
    void suppressionUniteProtegee() throws Exception {
        Decor d = preparer("LMD-H");

        mockMvc.perform(avecJeton(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/unites-enseignement/" + d.ue1), jetonAdmin))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------
    // Décor : deux UE, quatre ECUE, trente crédits
    // ------------------------------------------------------------------

    private record Decor(long ue1, long ue2, long ecueA, long ecueB, long ecueC, long ecueD,
                         String etudiantId, String jetonEnseignant, String jetonEtudiant) {}

    private JsonNode releve(Decor d) throws Exception {
        return donnees(mockMvc.perform(avecJeton(get("/moi/releve"), d.jetonEtudiant))
                .andExpect(status().isOk())
                .andReturn());
    }

    /** ECUE numéro {@code rangEcue} de l'UE numéro {@code rangUnite}. */
    private JsonNode ecue(JsonNode releve, int rangUnite, int rangEcue) {
        return releve.get("unites").get(rangUnite).get("ecues").get(rangEcue);
    }

    private void noter(Decor d, long matiereId, String valeur, String type) throws Exception {
        mockMvc.perform(avecJeton(post("/moi/notes"), d.jetonEnseignant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etudiantId":"%s","matiereId":%d,"valeur":%s,"coefficient":1,
                                 "libelle":"%s","dateEvaluation":"%s","type":"%s",
                                 "periode":"SEMESTRE_1"}
                                """.formatted(d.etudiantId, matiereId, valeur, type,
                                LocalDate.now(), type)))
                .andExpect(status().isCreated());
    }

    private Decor preparer(String cle) throws Exception {
        long promotionId = creer("/promotions", """
                {"code":"PROMO-%s","libelle":"Promotion %s","anneeAcademique":2026}
                """.formatted(cle, cle)).get("id").asLong();

        long classeId = creer("/classes", """
                {"code":"CL-%s","libelle":"Classe %s","promotionId":%d}
                """.formatted(cle, cle, promotionId)).get("id").asLong();

        String enseignantId = creer("/personnels", """
                {"matricule":"PROF-%s","nom":"Diarra","prenom":"Adama",
                 "type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();

        mockMvc.perform(avecJeton(put("/classes/" + classeId), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"CL-%s","libelle":"Classe %s","promotionId":%d,
                                 "enseignantIds":["%s"]}
                                """.formatted(cle, cle, promotionId, enseignantId)))
                .andExpect(status().isOk());

        // Crédits volontairement inégaux — 12 contre 18, 6 contre 9 : c'est le seul
        // moyen de distinguer une pondération par les crédits d'une moyenne simple.
        long ue1 = creer("/unites-enseignement", """
                {"code":"UE-%s-1","libelle":"Fondamentaux","credits":12,
                 "semestre":"SEMESTRE_1","promotionId":%d}
                """.formatted(cle, promotionId)).get("id").asLong();
        long ue2 = creer("/unites-enseignement", """
                {"code":"UE-%s-2","libelle":"Specialite","credits":18,
                 "semestre":"SEMESTRE_1","promotionId":%d}
                """.formatted(cle, promotionId)).get("id").asLong();

        long ecueA = ecue(cle, "A", 6, ue1);
        long ecueB = ecue(cle, "B", 6, ue1);
        long ecueC = ecue(cle, "C", 9, ue2);
        long ecueD = ecue(cle, "D", 9, ue2);

        String etudiantId = creer("/etudiants", """
                {"matricule":"%s-ET","nom":"Sangare","prenom":"Kadiatou",
                 "classeId":%d,"actif":true}
                """.formatted(cle, classeId)).get("id").asText();

        return new Decor(ue1, ue2, ecueA, ecueB, ecueC, ecueD, etudiantId,
                jetonDe("/personnels/" + enseignantId + "/compte"),
                jetonDe("/etudiants/" + etudiantId + "/compte"));
    }

    private long ecue(String cle, String rang, int credits, long uniteId) throws Exception {
        return creer("/matieres", """
                {"code":"EC-%s-%s","libelle":"ECUE %s","credits":%d,"uniteEnseignementId":%d}
                """.formatted(cle, rang, rang, credits, uniteId)).get("id").asLong();
    }

    private String jetonDe(String cheminCompte) throws Exception {
        JsonNode compte = creer(cheminCompte, null);
        return seConnecter(compte.get("email").asText(),
                compte.get("motDePasseInitial").asText());
    }

    private JsonNode creer(String chemin, String corps) throws Exception {
        var requete = avecJeton(post(chemin), jetonAdmin);
        if (corps != null) {
            requete = requete.contentType(MediaType.APPLICATION_JSON).content(corps);
        }
        return donnees(mockMvc.perform(requete).andExpect(status().isCreated()).andReturn());
    }
}
