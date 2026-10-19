package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Supports de cours : publication par l'enseignant, consultation par les étudiants de la classe. */
@DisplayName("Intégration — supports de cours")
class SupportsCoursTest extends IntegrationTestBase {

    private static final byte[] PDF = "%PDF-1.4 contenu de test".getBytes(StandardCharsets.UTF_8);

    @Test
    @DisplayName("publication, liste étudiante, téléchargement identique et notification")
    void cycleComplet() throws Exception {
        Decor d = preparer("SUP-A");
        JsonNode support = publier(d, d.jeton, "cours-1.pdf", PDF, "Chapitre 1", 201);
        assertThat(support.get("typeMime").asText()).isEqualTo("application/pdf");
        assertThat(support.get("taille").asLong()).isEqualTo(PDF.length);
        String id = support.get("id").asText();

        assertThat(appeler(get("/moi/classes/" + d.classeId + "/supports"), d.jeton, 200)).hasSize(1);
        JsonNode vus = appeler(get("/moi/supports"), d.jetonEtudiant, 200);
        assertThat(vus).hasSize(1);
        assertThat(vus.get(0).get("titre").asText()).isEqualTo("Chapitre 1");
        assertThat(appeler(get("/moi/supports"), d.jetonAutreClasse, 200)).isEmpty();

        MvcResult fichier = mockMvc.perform(avecJeton(get("/moi/supports/" + id + "/fichier"), d.jetonEtudiant))
                .andExpect(status().isOk()).andReturn();
        assertThat(fichier.getResponse().getContentAsByteArray()).isEqualTo(PDF);
        assertThat(fichier.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(fichier.getResponse().getHeader("Content-Disposition")).contains("attachment").contains("cours-1.pdf");

        assertThat(appeler(get("/notifications"), d.jetonEtudiant, 200).get(0).get("titre").asText())
                .startsWith("Nouveau support");
    }

    @Test
    @DisplayName("cloisonnement : ni téléchargement, ni suppression, ni publication hors de ses droits")
    void cloisonnement() throws Exception {
        Decor d = preparer("SUP-B");
        String id = publier(d, d.jeton, "notes.docx", PDF, "Notes", 201).get("id").asText();

        // Un étudiant d'une autre classe ne doit même pas savoir que le support existe.
        mockMvc.perform(avecJeton(get("/moi/supports/" + id + "/fichier"), d.jetonAutreClasse))
                .andExpect(status().isNotFound());
        mockMvc.perform(avecJeton(get("/moi/supports/" + id + "/fichier"), d.jetonCollegue))
                .andExpect(status().isForbidden());
        appeler(delete("/moi/supports/" + id), d.jetonCollegue, 403);
        appeler(delete("/moi/supports/" + id), d.jetonEtudiant, 403);
        // Un enseignant ne publie que dans une classe où il intervient.
        publier(d, d.jetonHorsClasse, "x.pdf", PDF, "Intrus", 403);
        // Un étudiant ne publie pas.
        publier(d, d.jetonEtudiant, "x.pdf", PDF, "Étudiant", 403);

        appeler(delete("/moi/supports/" + id), d.jeton, 200);
        mockMvc.perform(avecJeton(get("/moi/supports/" + id + "/fichier"), d.jeton)).andExpect(status().isNotFound());
        assertThat(appeler(get("/moi/supports"), d.jetonEtudiant, 200)).isEmpty();
    }

    @Test
    @DisplayName("formats non listés, fichiers vides et titre absent sont refusés")
    void validations() throws Exception {
        Decor d = preparer("SUP-C");
        publier(d, d.jeton, "script.exe", PDF, "Exécutable", 400);
        publier(d, d.jeton, "page.html", PDF, "HTML", 400);
        publier(d, d.jeton, "vide.pdf", new byte[0], "Vide", 400);
        publier(d, d.jeton, "ok.pdf", PDF, " ", 400);
        // Seul le nom du fichier est gardé, jamais le chemin envoyé.
        JsonNode support = publier(d, d.jeton, "../../etc/passwd.pdf", PDF, "Chemin", 201);
        assertThat(support.get("nomFichier").asText()).isEqualTo("passwd.pdf");
    }

    // ------------------------------------------------------------------

    private record Decor(long classeId, long matiereId, String jeton, String jetonCollegue,
                         String jetonHorsClasse, String jetonEtudiant, String jetonAutreClasse) { }

    private JsonNode publier(Decor d, String jeton, String nom, byte[] octets, String titre, int attendu)
            throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", nom, "application/octet-stream", octets);
        MvcResult r = mockMvc.perform(multipart("/moi/classes/" + d.classeId + "/supports").file(fichier)
                        .param("matiereId", String.valueOf(d.matiereId)).param("titre", titre)
                        .header("Authorization", "Bearer " + jeton))
                .andReturn();
        assertThat(r.getResponse().getStatus()).as("réponse : %s", r.getResponse().getContentAsString())
                .isEqualTo(attendu);
        String corps = r.getResponse().getContentAsString();
        return corps.isBlank() ? null : objectMapper.readTree(corps).get("data");
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
        String enseignantId = creer("/personnels", """
                {"matricule":"PROF-%s-1","nom":"Diallo","prenom":"Awa","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();
        String collegueId = creer("/personnels", """
                {"matricule":"PROF-%s-2","nom":"Sow","prenom":"Modibo","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();
        String horsClasseId = creer("/personnels", """
                {"matricule":"PROF-%s-3","nom":"Bah","prenom":"Fanta","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();
        mockMvc.perform(avecJeton(put("/classes/" + classeId), jetonAdmin).contentType(MediaType.APPLICATION_JSON)
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
        String autreEtudiantId = creer("/etudiants", """
                {"matricule":"%s-ET2","nom":"Coulibaly","prenom":"Bakary","classeId":%d,"actif":true}
                """.formatted(cle, autreClasseId)).get("id").asText();

        return new Decor(classeId, matiereId,
                jetonDe("/personnels/" + enseignantId + "/compte"),
                jetonDe("/personnels/" + collegueId + "/compte"),
                jetonDe("/personnels/" + horsClasseId + "/compte"),
                jetonDe("/etudiants/" + etudiantId + "/compte"),
                jetonDe("/etudiants/" + autreEtudiantId + "/compte"));
    }

    private JsonNode appeler(MockHttpServletRequestBuilder requete, String jeton, int attendu) throws Exception {
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
