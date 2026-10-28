package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartpresence.repository.EmpruntRepository;
import com.smartpresence.service.BibliothequeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Bibliothèque : dépôts modérés, catalogue, emprunts, retards et rappels. */
@DisplayName("Intégration — bibliothèque")
class BibliothequeTest extends IntegrationTestBase {

    private static final byte[] PDF = "%PDF-1.4 memoire de test".getBytes(StandardCharsets.UTF_8);

    @Autowired private EmpruntRepository empruntRepository;
    @Autowired private BibliothequeService bibliotheque;

    @Test
    @DisplayName("un dépôt d'étudiant attend la décision, puis devient visible et téléchargeable")
    void depotModere() throws Exception {
        Decor d = preparer("BIB-A");
        JsonNode depot = deposer(d.jetonEtudiant, "MEMOIRE", "Memoire reseaux BIBA", "Keita Sira", null, 201);
        assertThat(depot.get("statut").asText()).isEqualTo("EN_ATTENTE");
        String id = depot.get("id").asText();

        // Invisible du catalogue, du téléchargement des autres, mais visible de son auteur.
        assertThat(titres(appeler(get("/bibliotheque/documents"), d.jetonAutre, 200))).doesNotContain("Memoire reseaux BIBA");
        mockMvc.perform(avecJeton(get("/bibliotheque/documents/" + id + "/fichier"), d.jetonAutre))
                .andExpect(status().isNotFound());
        mockMvc.perform(avecJeton(get("/bibliotheque/documents/" + id + "/fichier"), d.jetonEtudiant))
                .andExpect(status().isOk());
        assertThat(titres(appeler(get("/bibliotheque/mes-depots"), d.jetonEtudiant, 200))).contains("Memoire reseaux BIBA");
        assertThat(titres(appeler(get("/bibliotheque/documents/a-moderer"), jetonAdmin, 200))).contains("Memoire reseaux BIBA");

        // Seule l'administration décide ; un refus exige un motif.
        decider(d.jetonEtudiant, id, "{\"accepte\":true}", 403);
        decider(jetonAdmin, id, "{\"accepte\":false}", 400);
        decider(jetonAdmin, id, "{\"accepte\":true}", 200);
        decider(jetonAdmin, id, "{\"accepte\":true}", 409);

        assertThat(titres(appeler(get("/bibliotheque/documents"), d.jetonAutre, 200))).contains("Memoire reseaux BIBA");
        MvcResult fichier = mockMvc.perform(avecJeton(get("/bibliotheque/documents/" + id + "/fichier"), d.jetonAutre))
                .andExpect(status().isOk()).andReturn();
        assertThat(fichier.getResponse().getContentAsByteArray()).isEqualTo(PDF);
        assertThat(appeler(get("/notifications"), d.jetonEtudiant, 200).get(0).get("titre").asText())
                .isEqualTo("Document publié");

        // Un document publié ne se retire plus par son auteur.
        appeler(delete("/bibliotheque/documents/" + id), d.jetonEtudiant, 403);
        appeler(delete("/bibliotheque/documents/" + id), jetonAdmin, 200);
    }

    @Test
    @DisplayName("un dépôt refusé garde son motif et peut être retiré par son auteur")
    void depotRefuse() throws Exception {
        Decor d = preparer("BIB-B");
        String id = deposer(d.jetonEtudiant, "SUJET", "Examen BIBB", null, "Algorithmique", 201).get("id").asText();
        decider(jetonAdmin, id, "{\"accepte\":false,\"motif\":\"Scan illisible\"}", 200);
        JsonNode mes = appeler(get("/bibliotheque/mes-depots"), d.jetonEtudiant, 200);
        assertThat(mes.get(0).get("statut").asText()).isEqualTo("REFUSE");
        assertThat(mes.get(0).get("motifRefus").asText()).isEqualTo("Scan illisible");
        appeler(delete("/bibliotheque/documents/" + id), d.jetonAutre, 404);
        appeler(delete("/bibliotheque/documents/" + id), d.jetonEtudiant, 200);
        assertThat(appeler(get("/bibliotheque/mes-depots"), d.jetonEtudiant, 200)).isEmpty();
    }

    @Test
    @DisplayName("l'administration publie d'emblée ; filtres, droits et validations")
    void publicationAdministrationEtFiltres() throws Exception {
        Decor d = preparer("BIB-C");
        JsonNode sujet = deposer(jetonAdmin, "SUJET", "Examen reseaux BIBC", null, "Reseaux", 201);
        assertThat(sujet.get("statut").asText()).isEqualTo("PUBLIE");
        deposer(jetonAdmin, "MEMOIRE", "Memoire securite BIBC", "Sow Modibo", null, 201);

        assertThat(titres(appeler(get("/bibliotheque/documents?type=SUJET&q=reseaux"), d.jetonEtudiant, 200)))
                .contains("Examen reseaux BIBC").doesNotContain("Memoire securite BIBC");
        assertThat(titres(appeler(get("/bibliotheque/documents?q=SECURITE"), d.jetonEtudiant, 200)))
                .containsExactly("Memoire securite BIBC");

        // L'enseignant consulte, il ne dépose pas ; les autres rôles non plus.
        assertThat(appeler(get("/bibliotheque/documents"), d.jetonEnseignant, 200)).isNotEmpty();
        deposer(d.jetonEnseignant, "SUJET", "Intrus", null, "Reseaux", 403);
        appeler(get("/bibliotheque/documents/a-moderer"), d.jetonEtudiant, 403);

        deposer(jetonAdmin, "MEMOIRE", "Sans auteur", null, null, 400);
        deposer(jetonAdmin, "SUJET", "Sans matiere", null, null, 400);
        deposerFichier(jetonAdmin, "MEMOIRE", "Executable", "A B", null, "virus.exe", PDF, 400);
        deposerFichier(jetonAdmin, "MEMOIRE", "Vide", "A B", null, "vide.pdf", new byte[0], 400);
        deposerFichier(jetonAdmin, "MEMOIRE", " ", "A B", null, "ok.pdf", PDF, 400);
    }

    @Test
    @DisplayName("emprunt, exemplaires disponibles, limites et retour")
    void cycleEmprunt() throws Exception {
        Decor d = preparer("BIB-D");
        String livre = creerLivre("Algorithmique BIBD", 1);
        assertThat(disponibles(d.jetonEtudiant, livre)).isEqualTo(1);

        JsonNode emprunt = emprunter(jetonAdmin, livre, d.etudiantId, 201);
        assertThat(emprunt.get("statut").asText()).isEqualTo("EN_COURS");
        assertThat(LocalDate.parse(emprunt.get("retourPrevu").asText()))
                .isEqualTo(LocalDate.now().plusDays(14));
        assertThat(disponibles(d.jetonEtudiant, livre)).isEqualTo(0);

        // Dernier exemplaire sorti : refus, y compris pour un autre étudiant.
        emprunter(jetonAdmin, livre, d.autreEtudiantId, 409);
        // Ni l'étudiant ni l'enseignant ne tiennent les emprunts.
        emprunter(d.jetonEtudiant, livre, d.etudiantId, 403);
        emprunter(d.jetonEnseignant, livre, d.etudiantId, 403);
        appeler(get("/bibliotheque/emprunts"), d.jetonEtudiant, 403);

        JsonNode mes = appeler(get("/bibliotheque/mes-emprunts"), d.jetonEtudiant, 200);
        assertThat(mes).hasSize(1);
        assertThat(appeler(get("/bibliotheque/mes-emprunts"), d.jetonAutre, 200)).isEmpty();
        assertThat(appeler(get("/notifications"), d.jetonEtudiant, 200).get(0).get("titre").asText())
                .isEqualTo("Livre emprunté");

        String id = emprunt.get("id").asText();
        appeler(post("/bibliotheque/emprunts/" + id + "/retour"), jetonAdmin, 200);
        appeler(post("/bibliotheque/emprunts/" + id + "/retour"), jetonAdmin, 409);
        assertThat(disponibles(d.jetonEtudiant, livre)).isEqualTo(1);
        assertThat(appeler(get("/bibliotheque/mes-emprunts"), d.jetonEtudiant, 200).get(0).get("statut").asText())
                .isEqualTo("RETOURNE");

        // Un livre qui a un historique ne se supprime pas ; on ne descend pas sous les exemplaires sortis.
        appeler(delete("/bibliotheque/livres/" + livre), jetonAdmin, 409);
    }

    @Test
    @DisplayName("trois livres au plus par étudiant, pas de doublon, durée choisie")
    void limitesDEmprunt() throws Exception {
        Decor d = preparer("BIB-E");
        String a = creerLivre("Livre A BIBE", 2);
        String b = creerLivre("Livre B BIBE", 2);
        String c = creerLivre("Livre C BIBE", 2);
        String e = creerLivre("Livre D BIBE", 2);

        JsonNode court = emprunter(jetonAdmin, a, d.etudiantId, 201, 3);
        assertThat(LocalDate.parse(court.get("retourPrevu").asText())).isEqualTo(LocalDate.now().plusDays(3));
        emprunter(jetonAdmin, a, d.etudiantId, 409);
        emprunter(jetonAdmin, b, d.etudiantId, 201);
        emprunter(jetonAdmin, c, d.etudiantId, 201);
        emprunter(jetonAdmin, e, d.etudiantId, 409);

        // Le total ne peut pas passer sous le nombre d'exemplaires sortis.
        mockMvc.perform(avecJeton(put("/bibliotheque/livres/" + a), jetonAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content(livreJson("Livre A BIBE", 0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("un retard bloque les nouveaux emprunts et les prolongations, et déclenche un rappel")
    void retardEtRappels() throws Exception {
        Decor d = preparer("BIB-F");
        String livre = creerLivre("Livre en retard BIBF", 2);
        String autre = creerLivre("Autre livre BIBF", 2);
        String id = emprunter(jetonAdmin, livre, d.etudiantId, 201).get("id").asText();

        // Prolongation normale : la date recule et le compteur monte, deux fois au plus.
        appeler(post("/bibliotheque/emprunts/" + id + "/prolongation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"jours\":7}"), jetonAdmin, 200);
        JsonNode prolonge = appeler(post("/bibliotheque/emprunts/" + id + "/prolongation")
                .contentType(MediaType.APPLICATION_JSON).content("{\"jours\":7}"), jetonAdmin, 200);
        assertThat(prolonge.get("prolongations").asInt()).isEqualTo(2);
        appeler(post("/bibliotheque/emprunts/" + id + "/prolongation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"jours\":7}"), jetonAdmin, 409);

        // On fait vieillir l'emprunt de 40 jours au-delà de sa date de retour.
        var entite = empruntRepository.findById(UUID.fromString(id)).orElseThrow();
        entite.setRetourPrevu(LocalDate.now().minusDays(5));
        empruntRepository.save(entite);

        JsonNode liste = appeler(get("/bibliotheque/emprunts"), jetonAdmin, 200);
        JsonNode ligne = null;
        for (JsonNode n : liste) {
            if (n.get("id").asText().equals(id)) ligne = n;
        }
        assertThat(ligne).isNotNull();
        assertThat(ligne.get("statut").asText()).isEqualTo("EN_RETARD");
        assertThat(ligne.get("joursDeRetard").asLong()).isEqualTo(5);
        assertThat(appeler(get("/bibliotheque/synthese"), jetonAdmin, 200).get("empruntsEnRetard").asLong())
                .isGreaterThanOrEqualTo(1);

        emprunter(jetonAdmin, autre, d.etudiantId, 409);
        appeler(post("/bibliotheque/emprunts/" + id + "/prolongation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"jours\":7}"), jetonAdmin, 409);

        // Un rappel part une fois, pas deux le même jour.
        assertThat(bibliotheque.envoyerRappels()).isGreaterThanOrEqualTo(1);
        assertThat(bibliotheque.envoyerRappels()).isZero();
        assertThat(appeler(get("/notifications"), d.jetonEtudiant, 200).get(0).get("titre").asText())
                .isEqualTo("Livre en retard");

        // Le retour rétablit le droit d'emprunter.
        appeler(post("/bibliotheque/emprunts/" + id + "/retour"), jetonAdmin, 200);
        emprunter(jetonAdmin, autre, d.etudiantId, 201);
    }

    // ------------------------------------------------------------------

    private record Decor(String jetonEtudiant, String jetonAutre, String jetonEnseignant,
                         String etudiantId, String autreEtudiantId) { }

    private int disponibles(String jeton, String livreId) throws Exception {
        for (JsonNode l : appeler(get("/bibliotheque/livres"), jeton, 200)) {
            if (l.get("id").asText().equals(livreId)) return l.get("disponibles").asInt();
        }
        throw new AssertionError("livre introuvable : " + livreId);
    }

    private String livreJson(String titre, int exemplaires) {
        return """
                {"titre":"%s","auteur":"Auteur Test","categorie":"Informatique","exemplaires":%d}
                """.formatted(titre, exemplaires);
    }

    private String creerLivre(String titre, int exemplaires) throws Exception {
        return donnees(mockMvc.perform(avecJeton(post("/bibliotheque/livres"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(livreJson(titre, exemplaires)))
                .andExpect(status().isCreated()).andReturn()).get("id").asText();
    }

    private JsonNode emprunter(String jeton, String livreId, String etudiantId, int attendu) throws Exception {
        return emprunter(jeton, livreId, etudiantId, attendu, null);
    }

    private JsonNode emprunter(String jeton, String livreId, String etudiantId, int attendu, Integer duree)
            throws Exception {
        String corps = duree == null
                ? "{\"livreId\":\"%s\",\"etudiantId\":\"%s\"}".formatted(livreId, etudiantId)
                : "{\"livreId\":\"%s\",\"etudiantId\":\"%s\",\"dureeJours\":%d}".formatted(livreId, etudiantId, duree);
        return appeler(post("/bibliotheque/emprunts").contentType(MediaType.APPLICATION_JSON).content(corps),
                jeton, attendu);
    }

    private void decider(String jeton, String id, String corps, int attendu) throws Exception {
        appeler(post("/bibliotheque/documents/" + id + "/decision").contentType(MediaType.APPLICATION_JSON)
                .content(corps), jeton, attendu);
    }

    private JsonNode deposer(String jeton, String type, String titre, String auteur, String matiere, int attendu)
            throws Exception {
        return deposerFichier(jeton, type, titre, auteur, matiere, "document.pdf", PDF, attendu);
    }

    private JsonNode deposerFichier(String jeton, String type, String titre, String auteur, String matiere,
                                    String nom, byte[] octets, int attendu) throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", nom, "application/octet-stream", octets);
        var requete = multipart("/bibliotheque/documents").file(fichier).param("type", type).param("titre", titre)
                .header("Authorization", "Bearer " + jeton);
        if (auteur != null) requete.param("auteur", auteur);
        if (matiere != null) requete.param("matiere", matiere);
        MvcResult r = mockMvc.perform(requete).andReturn();
        assertThat(r.getResponse().getStatus()).as("réponse : %s", r.getResponse().getContentAsString())
                .isEqualTo(attendu);
        String corps = r.getResponse().getContentAsString();
        return corps.isBlank() ? null : objectMapper.readTree(corps).get("data");
    }

    private static java.util.List<String> titres(JsonNode documents) {
        java.util.List<String> titres = new java.util.ArrayList<>();
        documents.forEach(n -> titres.add(n.get("titre").asText()));
        return titres;
    }

    private Decor preparer(String cle) throws Exception {
        long promotionId = creer("/promotions", """
                {"code":"PROMO-%s","libelle":"Promotion %s","anneeAcademique":2026}
                """.formatted(cle, cle)).get("id").asLong();
        long classeId = creer("/classes", """
                {"code":"%s","libelle":"Classe %s","promotionId":%d}
                """.formatted(cle, cle, promotionId)).get("id").asLong();
        String enseignantId = creer("/personnels", """
                {"matricule":"PROF-%s","nom":"Diallo","prenom":"Awa","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();
        String etudiantId = creer("/etudiants", """
                {"matricule":"%s-ET1","nom":"Keita","prenom":"Sira","classeId":%d,"actif":true}
                """.formatted(cle, classeId)).get("id").asText();
        String autreId = creer("/etudiants", """
                {"matricule":"%s-ET2","nom":"Coulibaly","prenom":"Bakary","classeId":%d,"actif":true}
                """.formatted(cle, classeId)).get("id").asText();
        return new Decor(jetonDe("/etudiants/" + etudiantId + "/compte"), jetonDe("/etudiants/" + autreId + "/compte"),
                jetonDe("/personnels/" + enseignantId + "/compte"), etudiantId, autreId);
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
