package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Import en lot d'étudiants : simulation, bilan par ligne, doublons et droits. */
@DisplayName("Intégration — import d'étudiants")
class ImportEtudiantsTest extends IntegrationTestBase {

    @Test
    @DisplayName("la simulation ne crée rien ; l'import crée, ignore les inscrits et signale les erreurs")
    void importComplet() throws Exception {
        long classeId = creerClasse("IMP-A");
        String lignes = """
                [
                  {"matricule":"imp-a-001","nom":"Traoré","prenom":"Moussa","email":"moussa.imp@exemple.ml"},
                  {"matricule":"IMP-A-002","nom":"Keita","prenom":"Sira"},
                  {"matricule":"","nom":"Sans","prenom":"Matricule"},
                  {"matricule":"IMP-A-003","nom":"Diallo","prenom":"Awa","email":"pas-un-email"},
                  {"matricule":"IMP-A-002","nom":"Doublon","prenom":"Fichier"}
                ]
                """;

        JsonNode essai = importer(classeId, lignes, true, 200);
        assertThat(essai.get("simulation").asBoolean()).isTrue();
        assertThat(essai.get("creees").asInt()).isEqualTo(2);
        assertThat(essai.get("erreurs").asInt()).isEqualTo(3);
        assertThat(effectif(classeId)).isZero();

        JsonNode bilan = importer(classeId, lignes, false, 200);
        assertThat(bilan.get("creees").asInt()).isEqualTo(2);
        assertThat(bilan.get("lignes").get(0).get("matricule").asText()).isEqualTo("IMP-A-001");
        assertThat(bilan.get("lignes").get(2).get("message").asText()).isEqualTo("Matricule manquant");
        assertThat(bilan.get("lignes").get(3).get("message").asText()).isEqualTo("Email invalide");
        assertThat(bilan.get("lignes").get(4).get("message").asText()).contains("double");
        assertThat(effectif(classeId)).isEqualTo(2);

        // Relancer le même fichier ne crée rien de plus : les inscrits sont ignorés.
        JsonNode relance = importer(classeId, lignes, false, 200);
        assertThat(relance.get("creees").asInt()).isZero();
        assertThat(relance.get("ignorees").asInt()).isEqualTo(2);
        assertThat(effectif(classeId)).isEqualTo(2);
    }

    @Test
    @DisplayName("classe inconnue, liste vide et trop longue sont refusées")
    void validations() throws Exception {
        long classeId = creerClasse("IMP-B");
        importer(999999L, "[{\"matricule\":\"X1\",\"nom\":\"A\",\"prenom\":\"B\"}]", false, 404);
        importer(classeId, "[]", false, 400);

        StringBuilder trop = new StringBuilder("[");
        for (int i = 0; i < 501; i++) {
            trop.append(i == 0 ? "" : ",").append("{\"matricule\":\"T").append(i).append("\",\"nom\":\"A\",\"prenom\":\"B\"}");
        }
        importer(classeId, trop.append("]").toString(), true, 400);
    }

    @Test
    @DisplayName("un étudiant ou un enseignant n'importe pas de liste")
    void droits() throws Exception {
        long classeId = creerClasse("IMP-C");
        String compte = donnees(mockMvc.perform(avecJeton(post("/etudiants"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matricule\":\"IMP-C-ET\",\"nom\":\"Test\",\"prenom\":\"Droit\",\"classeId\":" + classeId + "}"))
                .andExpect(status().isCreated()).andReturn()).get("id").asText();
        JsonNode acces = donnees(mockMvc.perform(avecJeton(post("/etudiants/" + compte + "/compte"), jetonAdmin))
                .andExpect(status().isCreated()).andReturn());
        String jetonEtudiant = seConnecter(acces.get("email").asText(), acces.get("motDePasseInitial").asText());

        mockMvc.perform(avecJeton(post("/etudiants/import"), jetonEtudiant).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classeId\":" + classeId + ",\"etudiants\":[{\"matricule\":\"Z1\",\"nom\":\"A\",\"prenom\":\"B\"}]}"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------

    private JsonNode importer(long classeId, String lignes, boolean simulation, int attendu) throws Exception {
        var r = mockMvc.perform(avecJeton(post("/etudiants/import"), jetonAdmin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classeId\":" + classeId + ",\"simulation\":" + simulation + ",\"etudiants\":" + lignes + "}"))
                .andReturn();
        assertThat(r.getResponse().getStatus()).as("réponse : %s", r.getResponse().getContentAsString())
                .isEqualTo(attendu);
        return attendu == 200 ? donnees(r) : null;
    }

    private int effectif(long classeId) throws Exception {
        return donnees(mockMvc.perform(avecJeton(get("/etudiants?classeId=" + classeId), jetonAdmin))
                .andExpect(status().isOk()).andReturn()).size();
    }

    private long creerClasse(String cle) throws Exception {
        long promotionId = donnees(mockMvc.perform(avecJeton(post("/promotions"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"PROMO-" + cle + "\",\"libelle\":\"Promotion " + cle + "\",\"anneeAcademique\":2026}"))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        return donnees(mockMvc.perform(avecJeton(post("/classes"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + cle + "\",\"libelle\":\"Classe " + cle + "\",\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }
}
