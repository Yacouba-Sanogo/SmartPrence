package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cloisonnement du rôle enseignant.
 *
 * <p>Le rôle {@code ENSEIGNANT} était traité comme un rôle d'administration sur six
 * routes, sans qu'aucune ne vérifie que la donnée demandée le concernait. Un enseignant
 * pouvait lister tous les étudiants de l'établissement, lire les statistiques et les
 * justificatifs d'absence de n'importe lequel, et créer des relevés de présence à la
 * main.</p>
 *
 * <p>Ces tests fixent la frontière : l'enseignant accède à <b>ses</b> classes par
 * l'espace {@code /moi}, et à rien d'autre.</p>
 */
@DisplayName("Intégration — cloisonnement du rôle enseignant")
class CloisonnementEnseignantTest extends IntegrationTestBase {

    @Test
    @DisplayName("un enseignant obtient l'effectif de sa classe")
    void effectifDeSaClasse() throws Exception {
        Contexte c = preparer("CLO-A");

        JsonNode effectif = donnees(mockMvc.perform(
                        avecJeton(get("/moi/classes/" + c.classeId + "/etudiants"), c.jetonEnseignant))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(effectif).hasSize(2);
        JsonNode etudiant = effectif.get(0);
        assertThat(etudiant.get("matricule").asText()).isNotBlank();
        assertThat(etudiant.get("enrole").asBoolean()).isFalse();

        // La réponse est volontairement pauvre : faire cours n'exige ni email,
        // ni téléphone, ni référence biométrique.
        assertThat(etudiant.has("biometricId")).isFalse();
        assertThat(etudiant.has("email")).isFalse();
        assertThat(etudiant.has("telephone")).isFalse();
    }

    @Test
    @DisplayName("un enseignant se voit refuser l'effectif d'une classe qui n'est pas la sienne")
    void effectifDUneAutreClasse() throws Exception {
        Contexte sien = preparer("CLO-B");
        Contexte autre = preparer("CLO-C");

        mockMvc.perform(avecJeton(
                        get("/moi/classes/" + autre.classeId + "/etudiants"), sien.jetonEnseignant))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un enseignant ne peut plus lister les étudiants de l'établissement")
    void listeGlobaleFermee() throws Exception {
        Contexte c = preparer("CLO-D");

        mockMvc.perform(avecJeton(get("/etudiants"), c.jetonEnseignant))
                .andExpect(status().isForbidden());
        mockMvc.perform(avecJeton(
                        get("/etudiants").param("classeId", String.valueOf(c.classeId)),
                        c.jetonEnseignant))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un enseignant ne peut plus parcourir toutes les classes")
    void catalogueDesClassesFerme() throws Exception {
        Contexte c = preparer("CLO-E");

        mockMvc.perform(avecJeton(get("/classes"), c.jetonEnseignant))
                .andExpect(status().isForbidden());
        mockMvc.perform(avecJeton(get("/classes/" + c.classeId), c.jetonEnseignant))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un enseignant ne peut plus créer un relevé de présence à la main")
    void saisieManuelleFermee() throws Exception {
        Contexte c = preparer("CLO-F");

        // Le corps est volontairement complet et valide : un corps incomplet
        // provoquerait un 400 de validation, et le test passerait sans rien prouver
        // de l'autorisation — y compris si le rôle était réattribué.
        String requeteValide = """
                {"etudiantId":"%s","seanceId":"%s","datePresence":"%s",
                 "heurePresence":"08:05:00","statut":"PRESENT"}
                """.formatted(c.etudiantId, c.seanceId, java.time.LocalDate.now());

        // Le circuit prévu est le signalement : l'enseignant témoigne, la scolarité tranche.
        mockMvc.perform(avecJeton(post("/presences/manual"), c.jetonEnseignant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requeteValide))
                .andExpect(status().isForbidden());

        // La même requête aboutit pour la scolarité : c'est bien le rôle qui tranche.
        mockMvc.perform(avecJeton(post("/presences/manual"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requeteValide))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("un enseignant ne peut plus lire l'assiduité ni les justificatifs d'un étudiant")
    void dossiersIndividuelsFermes() throws Exception {
        Contexte c = preparer("CLO-G");

        mockMvc.perform(avecJeton(get("/statistiques/etudiants/" + c.etudiantId), c.jetonEnseignant))
                .andExpect(status().isForbidden());
        mockMvc.perform(avecJeton(get("/justifications/etudiant/" + c.etudiantId), c.jetonEnseignant))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("l'espace /moi reste ouvert : le cloisonnement n'a rien cassé")
    void espaceMoiIntact() throws Exception {
        Contexte c = preparer("CLO-H");

        mockMvc.perform(avecJeton(get("/moi/classes"), c.jetonEnseignant))
                .andExpect(status().isOk());
        mockMvc.perform(avecJeton(get("/moi/seances"), c.jetonEnseignant))
                .andExpect(status().isOk());
        mockMvc.perform(avecJeton(get("/moi/seances/" + c.seanceId + "/feuille"), c.jetonEnseignant))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------

    private record Contexte(long classeId, String etudiantId, String seanceId,
                            String jetonEnseignant) {}

    private Contexte preparer(String cle) throws Exception {
        long promotionId = creer("/promotions", """
                {"code":"PROMO-%s","libelle":"Promotion %s","anneeAcademique":2026}
                """.formatted(cle, cle)).get("id").asLong();

        long classeId = creer("/classes", """
                {"code":"%s","libelle":"Classe %s","promotionId":%d}
                """.formatted(cle, cle, promotionId)).get("id").asLong();

        String enseignantId = creer("/personnels", """
                {"matricule":"PROF-%s","nom":"Coulibaly","prenom":"Sekou","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();

        mockMvc.perform(avecJeton(put("/classes/" + classeId), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","libelle":"Classe %s","promotionId":%d,"enseignantIds":["%s"]}
                                """.formatted(cle, cle, promotionId, enseignantId)))
                .andExpect(status().isOk());

        String etudiantId = null;
        for (int i = 1; i <= 2; i++) {
            String id = creer("/etudiants", """
                    {"matricule":"%s-ET%d","nom":"Bamba","prenom":"Etudiant%d",
                     "classeId":%d,"actif":true}
                    """.formatted(cle, i, i, classeId)).get("id").asText();
            if (etudiantId == null) etudiantId = id;
        }

        long matiereId = creer("/matieres", """
                {"code":"MAT-%s","libelle":"Matiere %s","credits":3}
                """.formatted(cle, cle)).get("id").asLong();

        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        String seanceId = creer("/seances", """
                {"classeId":%d,"matiereId":%d,"enseignantId":"%s","debut":"%s","fin":"%s"}
                """.formatted(classeId, matiereId, enseignantId,
                java.time.LocalDate.now().atTime(8, 0).atZone(zone).toInstant(),
                java.time.LocalDate.now().atTime(10, 0).atZone(zone).toInstant()))
                .get("id").asText();

        JsonNode compte = creer("/personnels/" + enseignantId + "/compte", null);
        String jeton = seConnecter(
                compte.get("email").asText(), compte.get("motDePasseInitial").asText());

        return new Contexte(classeId, etudiantId, seanceId, jeton);
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
