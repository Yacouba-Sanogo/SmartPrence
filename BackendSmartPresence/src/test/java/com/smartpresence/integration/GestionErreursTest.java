package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Traduction des fautes de requête en réponses exploitables.
 *
 * <p>Trois familles d'erreurs tombaient dans le filet général et renvoyaient
 * « 500 — une erreur interne du serveur est survenue » : un corps JSON malformé, un
 * paramètre obligatoire absent, un paramètre du mauvais type. Toutes sont des fautes
 * côté client, et un 500 envoie le développeur chercher le problème sur le serveur.</p>
 */
@DisplayName("Intégration — traduction des erreurs de requête")
class GestionErreursTest extends IntegrationTestBase {

    @Test
    @DisplayName("un instant sans fuseau donne 400, pas 500")
    void corpsMalformeDonne400() throws Exception {
        MvcResult resultat = mockMvc.perform(avecJeton(post("/seances"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"classeId":1,"matiereId":1,
                                 "enseignantId":"00000000-0000-0000-0000-000000000000",
                                 "debut":"2026-08-20T08:00:00","fin":"2026-08-20T10:00:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(message(resultat))
                .as("le message doit nommer le champ fautif et le format attendu")
                .contains("debut")
                .contains("fuseau");
    }

    @Test
    @DisplayName("une valeur d'énumération inconnue nomme le champ et liste les valeurs")
    void enumInconnueNommeLesValeursAcceptees() throws Exception {
        // Ce cas renvoyait « les instants doivent porter un fuseau » pour un champ qui
        // n'est pas une date : le conseil envoyait chercher une faute inexistante.
        MvcResult resultat = mockMvc.perform(avecJeton(post("/devices"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Lecteur essai","adresseMac":"AA:BB:CC:DD:EE:FF",
                                 "apiKey":"cle-essai","usage":"ETUDIANTS"}
                                """))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(message(resultat))
                .as("le message doit désigner le champ énuméré et ses valeurs, pas une date")
                .contains("usage")
                .contains("ETUDIANT, PERSONNEL, MIXTE")
                .doesNotContain("fuseau");
    }

    @Test
    @DisplayName("un paramètre obligatoire absent donne 400 et nomme le paramètre")
    void parametreManquantDonne400() throws Exception {
        MvcResult resultat = mockMvc.perform(avecJeton(post("/justifications"), jetonAdmin))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(message(resultat)).contains("etudiantId");
    }

    @Test
    @DisplayName("un paramètre du mauvais type donne 400 et nomme le paramètre")
    void parametreInvalideDonne400() throws Exception {
        MvcResult resultat = mockMvc.perform(avecJeton(
                        get("/seances").param("debut", "pas-une-date"), jetonAdmin))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(message(resultat)).contains("debut");
    }

    @Test
    @DisplayName("une ressource inexistante donne 404, pas 500")
    void ressourceInexistanteDonne404() throws Exception {
        mockMvc.perform(avecJeton(
                        get("/classes/999999"), jetonAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("une route inconnue donne 404, pas 500")
    void routeInconnueDonne404() throws Exception {
        // /moi existe comme préfixe mais pas comme route : Spring tombait alors dans le
        // filet général et répondait 500, ce qui fait chercher une panne inexistante.
        MvcResult resultat = mockMvc.perform(avecJeton(get("/moi"), jetonAdmin))
                .andExpect(status().isNotFound())
                .andReturn();

        assertThat(message(resultat))
                .as("le message doit dire que l'adresse n'existe pas")
                .contains("moi");
    }

    private String message(MvcResult resultat) throws Exception {
        JsonNode corps = objectMapper.readTree(resultat.getResponse().getContentAsString());
        return corps.get("message").asText();
    }
}
