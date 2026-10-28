package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Responsable de classe : désigné par l'administrateur parmi les étudiants de la classe. */
@DisplayName("Intégration — responsable de classe")
class ResponsableClasseTest extends IntegrationTestBase {

    @Test
    @DisplayName("désignation, visibilité dans la classe et sur le profil, retrait")
    void cycleComplet() throws Exception {
        Decor d = preparer("RESP-A");

        JsonNode classe = designer(d.classeId, d.etudiantA, jetonAdmin, 200);
        assertThat(classe.get("responsableId").asText()).isEqualTo(d.etudiantA);
        assertThat(classe.get("responsableNom").asText()).isEqualTo("Sira Keita");
        assertThat(appeler(get("/classes/" + d.classeId), jetonAdmin, 200).get("responsableNom").asText())
                .isEqualTo("Sira Keita");

        // Chacun voit le responsable ; seul lui sait qu'il l'est.
        JsonNode profilA = appeler(get("/moi/profil"), d.jetonA, 200);
        assertThat(profilA.get("responsableClasse").asBoolean()).isTrue();
        JsonNode profilB = appeler(get("/moi/profil"), d.jetonB, 200);
        assertThat(profilB.get("responsableClasse").asBoolean()).isFalse();
        assertThat(profilB.get("responsableClasseNom").asText()).isEqualTo("Sira Keita");

        // Le désigné est prévenu.
        assertThat(appeler(get("/notifications"), d.jetonA, 200).get(0).get("titre").asText())
                .isEqualTo("Vous êtes responsable de classe");

        // Changer de responsable prévient l'ancien.
        designer(d.classeId, d.etudiantB, jetonAdmin, 200);
        assertThat(appeler(get("/moi/profil"), d.jetonA, 200).get("responsableClasse").asBoolean()).isFalse();
        assertThat(appeler(get("/notifications"), d.jetonA, 200).get(0).get("titre").asText())
                .isEqualTo("Responsabilité de classe terminée");

        // Retrait explicite.
        JsonNode vide = designer(d.classeId, null, jetonAdmin, 200);
        assertThat(vide.get("responsableId").isNull()).isTrue();
        assertThat(appeler(get("/moi/profil"), d.jetonB, 200).get("responsableClasse").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("seul l'administrateur désigne, et seulement un étudiant actif de la classe")
    void droitsEtValidations() throws Exception {
        Decor d = preparer("RESP-B");

        designer(d.classeId, d.etudiantA, d.jetonA, 403);
        designer(d.classeId, d.etudiantEtranger, jetonAdmin, 409);
        designer(d.classeId, "00000000-0000-0000-0000-000000000000", jetonAdmin, 404);
        designer(999999L, d.etudiantA, jetonAdmin, 404);

        appeler(patch("/etudiants/" + d.etudiantB + "/active?value=false"),
                jetonAdmin, 200);
        designer(d.classeId, d.etudiantB, jetonAdmin, 409);
    }

    @Test
    @DisplayName("un responsable qui change de classe perd son rôle, même s'il revient")
    void changementDeClasse() throws Exception {
        Decor d = preparer("RESP-C");
        designer(d.classeId, d.etudiantA, jetonAdmin, 200);

        appeler(patch("/etudiants/" + d.etudiantA + "/classe").contentType(MediaType.APPLICATION_JSON)
                .content("{\"classeId\":" + d.autreClasseId + "}"), jetonAdmin, 200);
        assertThat(appeler(get("/classes/" + d.classeId), jetonAdmin, 200).get("responsableId").isNull()).isTrue();

        appeler(patch("/etudiants/" + d.etudiantA + "/classe").contentType(MediaType.APPLICATION_JSON)
                .content("{\"classeId\":" + d.classeId + "}"), jetonAdmin, 200);
        assertThat(appeler(get("/classes/" + d.classeId), jetonAdmin, 200).get("responsableId").isNull())
                .as("la désignation ne ressuscite pas au retour").isTrue();
    }

    // ------------------------------------------------------------------

    private record Decor(long classeId, long autreClasseId, String etudiantA, String etudiantB, String etudiantEtranger,
                         String jetonA, String jetonB) { }

    private JsonNode designer(long classeId, String etudiantId, String jeton, int attendu) throws Exception {
        String corps = etudiantId == null ? "{\"etudiantId\":null}" : "{\"etudiantId\":\"" + etudiantId + "\"}";
        return appeler(put("/classes/" + classeId + "/responsable").contentType(MediaType.APPLICATION_JSON).content(corps),
                jeton, attendu);
    }

    private Decor preparer(String cle) throws Exception {
        long promotionId = creer("/promotions", """
                {"code":"PROMO-%s","libelle":"Promotion %s","anneeAcademique":2026}
                """.formatted(cle, cle)).get("id").asLong();
        long classeId = creer("/classes", """
                {"code":"%s","libelle":"Classe %s","promotionId":%d}
                """.formatted(cle, cle, promotionId)).get("id").asLong();
        long autreClasseId = creer("/classes", """
                {"code":"%s-X","libelle":"Autre %s","promotionId":%d}
                """.formatted(cle, cle, promotionId)).get("id").asLong();
        String a = creer("/etudiants", """
                {"matricule":"%s-A","nom":"Keita","prenom":"Sira","classeId":%d,"actif":true}
                """.formatted(cle, classeId)).get("id").asText();
        String b = creer("/etudiants", """
                {"matricule":"%s-B","nom":"Coulibaly","prenom":"Bakary","classeId":%d,"actif":true}
                """.formatted(cle, classeId)).get("id").asText();
        String etranger = creer("/etudiants", """
                {"matricule":"%s-E","nom":"Diarra","prenom":"Ali","classeId":%d,"actif":true}
                """.formatted(cle, autreClasseId)).get("id").asText();
        return new Decor(classeId, autreClasseId, a, b, etranger, jetonDe("/etudiants/" + a + "/compte"),
                jetonDe("/etudiants/" + b + "/compte"));
    }

    private JsonNode appeler(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requete,
                             String jeton, int attendu) throws Exception {
        var r = mockMvc.perform(avecJeton(requete, jeton)).andReturn();
        assertThat(r.getResponse().getStatus()).as("réponse : %s", r.getResponse().getContentAsString())
                .isEqualTo(attendu);
        String corps = r.getResponse().getContentAsString();
        JsonNode data = corps.isBlank() ? null : objectMapper.readTree(corps).get("data");
        return data == null ? com.fasterxml.jackson.databind.node.NullNode.instance : data;
    }

    private String jetonDe(String chemin) throws Exception {
        JsonNode compte = creer(chemin, null);
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
