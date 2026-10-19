package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Demandes de congé des enseignants et contestations de notes des étudiants. */
@DisplayName("Intégration — congés et réclamations")
class CongesReclamationsTest extends IntegrationTestBase {

    // ------------------------------------------------------------------
    // Congés
    // ------------------------------------------------------------------

    @Test
    @DisplayName("demande, chevauchement refusé, décision de l'administration et notification")
    void cycleDuConge() throws Exception {
        Decor d = preparer("CGR-A");
        LocalDate debut = LocalDate.now().plusDays(10);
        Instant seance = debut.atTime(9, 0).atZone(java.time.ZoneId.systemDefault()).toInstant();
        creer("/seances", """
                {"classeId":%d,"matiereId":%d,"enseignantId":"%s","debut":"%s","fin":"%s"}
                """.formatted(d.classeId, d.matiereId, d.enseignantId, seance, seance.plus(Duration.ofHours(2))));

        JsonNode demande = appeler(post("/moi/conges").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("type", "CONGE", "debut", debut.toString(),
                        "fin", debut.plusDays(4).toString(), "motif", "Congé annuel"))), d.jeton, 201);
        assertThat(demande.get("statut").asText()).isEqualTo("EN_ATTENTE");
        assertThat(demande.get("seancesConcernees").asInt())
                .as("la demande doit dire combien de séances elle touche")
                .isEqualTo(1);
        String id = demande.get("id").asText();

        appeler(post("/moi/conges").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("debut", debut.plusDays(2).toString(),
                        "fin", debut.plusDays(6).toString(), "motif", "Chevauche"))), d.jeton, 409);
        appeler(post("/moi/conges").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("debut", debut.plusDays(9).toString(),
                        "fin", debut.plusDays(8).toString(), "motif", "Dates inversées"))), d.jeton, 400);

        // L'enseignant ne décide pas de sa propre demande.
        appeler(post("/conges/" + id + "/decision").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("accepter", true))), d.jeton, 403);

        JsonNode enAttente = appeler(get("/conges").param("statut", "EN_ATTENTE"), jetonAdmin, 200);
        assertThat(enAttente).anyMatch(c -> c.get("id").asText().equals(id));

        appeler(post("/conges/" + id + "/decision").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("accepter", false))), jetonAdmin, 400);
        JsonNode acceptee = appeler(post("/conges/" + id + "/decision").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("accepter", true, "reponse", "Bon repos"))), jetonAdmin, 200);
        assertThat(acceptee.get("statut").asText()).isEqualTo("ACCEPTEE");
        appeler(post("/conges/" + id + "/decision").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("accepter", true))), jetonAdmin, 409);

        JsonNode recues = appeler(get("/notifications"), d.jeton, 200);
        assertThat(recues.get(0).get("titre").asText()).isEqualTo("Demande acceptée");

        // Une demande traitée ne se retire plus.
        appeler(post("/moi/conges/" + id + "/annulation"), d.jeton, 409);
    }

    @Test
    @DisplayName("une demande en attente se retire, la demande d'un collègue non")
    void annulation() throws Exception {
        Decor d = preparer("CGR-B");
        LocalDate debut = LocalDate.now().plusDays(30);
        String id = appeler(post("/moi/conges").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("type", "FORMATION", "debut", debut.toString(),
                        "fin", debut.toString(), "motif", "Stage"))), d.jeton, 201).get("id").asText();

        appeler(post("/moi/conges/" + id + "/annulation"), d.jetonCollegue, 403);
        assertThat(appeler(post("/moi/conges/" + id + "/annulation"), d.jeton, 200)
                .get("statut").asText()).isEqualTo("ANNULEE");

        // La période est libérée : on peut redemander.
        appeler(post("/moi/conges").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("debut", debut.toString(), "fin", debut.toString(), "motif", "Reprise"))),
                d.jeton, 201);
        assertThat(appeler(get("/moi/conges"), d.jeton, 200)).hasSize(2);
        assertThat(appeler(get("/moi/conges"), d.jetonCollegue, 200)).isEmpty();
    }

    // ------------------------------------------------------------------
    // Réclamations
    // ------------------------------------------------------------------

    @Test
    @DisplayName("un étudiant conteste une note publiée ; l'enseignant la corrige, l'ancienne valeur est conservée")
    void contestationAcceptee() throws Exception {
        Decor d = preparer("CGR-C");
        String noteId = noterEtPublier(d, "8");

        JsonNode r = appeler(post("/moi/notes/" + noteId + "/reclamation").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("motif", "Question 3 non comptée"))), d.jetonEtudiant, 201);
        assertThat(r.get("statut").asText()).isEqualTo("OUVERTE");
        String id = r.get("id").asText();

        appeler(post("/moi/notes/" + noteId + "/reclamation").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("motif", "Encore"))), d.jetonEtudiant, 409);

        JsonNode recues = appeler(get("/moi/reclamations/recues"), d.jeton, 200);
        assertThat(recues).hasSize(1);
        assertThat(appeler(get("/moi/reclamations/recues"), d.jetonCollegue, 200)).isEmpty();
        assertThat(appeler(get("/notifications"), d.jeton, 200).get(0).get("titre").asText())
                .startsWith("Note contestée");

        appeler(post("/moi/reclamations/" + id + "/reponse").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("accepter", true, "reponse", "Vous avez raison", "valeurCorrigee", 11))),
                d.jetonCollegue, 403);
        JsonNode traitee = appeler(post("/moi/reclamations/" + id + "/reponse").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("accepter", true, "reponse", "Vous avez raison", "valeurCorrigee", 11))),
                d.jeton, 200);
        assertThat(traitee.get("statut").asText()).isEqualTo("ACCEPTEE");
        assertThat(traitee.get("valeurAvant").asDouble()).isEqualTo(8.0);
        assertThat(traitee.get("valeurApres").asDouble()).isEqualTo(11.0);

        JsonNode bulletin = appeler(get("/moi/bulletin"), d.jetonEtudiant, 200);
        assertThat(bulletin.get("moyenneGenerale").asDouble())
                .as("la correction doit se retrouver au bulletin")
                .isEqualTo(11.0);
        assertThat(appeler(get("/notifications"), d.jetonEtudiant, 200).get(0).get("titre").asText())
                .isEqualTo("Réclamation acceptée");

        appeler(post("/moi/reclamations/" + id + "/reponse").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("accepter", false, "reponse", "Trop tard"))), d.jeton, 409);
        assertThat(appeler(get("/moi/reclamations"), d.jetonEtudiant, 200).get(0).get("reponse").asText())
                .isEqualTo("Vous avez raison");
    }

    @Test
    @DisplayName("rejet : la note ne bouge pas ; brouillon et note d'autrui ne se contestent pas")
    void contestationRejetee() throws Exception {
        Decor d = preparer("CGR-D");
        String brouillon = saisirNote(d, "9");
        appeler(post("/moi/notes/" + brouillon + "/reclamation").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("motif", "Pas encore publiée"))), d.jetonEtudiant, 404);

        publierLesNotes(d.jeton, d.classeId);
        appeler(post("/moi/notes/" + brouillon + "/reclamation").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("motif", "Ce n'est pas ma note"))), d.jetonAutreEtudiant, 404);

        String id = appeler(post("/moi/notes/" + brouillon + "/reclamation").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("motif", "Trop sévère"))), d.jetonEtudiant, 201).get("id").asText();

        appeler(post("/moi/reclamations/" + id + "/reponse").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("accepter", false, "reponse", "Barème appliqué", "valeurCorrigee", 15))),
                d.jeton, 400);
        JsonNode rejetee = appeler(post("/moi/reclamations/" + id + "/reponse").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("accepter", false, "reponse", "Barème appliqué"))), d.jeton, 200);
        assertThat(rejetee.get("statut").asText()).isEqualTo("REJETEE");
        assertThat(appeler(get("/moi/bulletin"), d.jetonEtudiant, 200).get("moyenneGenerale").asDouble())
                .isEqualTo(9.0);

        // Une fois traitée, la note peut être contestée de nouveau.
        appeler(post("/moi/notes/" + brouillon + "/reclamation").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("motif", "Nouvel élément"))), d.jetonEtudiant, 201);
    }

    // ------------------------------------------------------------------

    private record Decor(long classeId, long matiereId, String enseignantId, String jeton,
                         String jetonCollegue, String jetonEtudiant, String jetonAutreEtudiant,
                         String etudiantId) { }

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
        String autreEtudiantId = creer("/etudiants", """
                {"matricule":"%s-ET2","nom":"Coulibaly","prenom":"Bakary","classeId":%d,"actif":true}
                """.formatted(cle, classeId)).get("id").asText();

        return new Decor(classeId, matiereId, enseignantId,
                jetonDe("/personnels/" + enseignantId + "/compte"),
                jetonDe("/personnels/" + collegueId + "/compte"),
                jetonDe("/etudiants/" + etudiantId + "/compte"),
                jetonDe("/etudiants/" + autreEtudiantId + "/compte"), etudiantId);
    }

    private String saisirNote(Decor d, String valeur) throws Exception {
        return appeler(post("/moi/notes").contentType(MediaType.APPLICATION_JSON).content("""
                {"etudiantId":"%s","matiereId":%d,"valeur":%s,"coefficient":1,
                 "libelle":"Devoir 1","dateEvaluation":"%s","type":"DEVOIR"}
                """.formatted(d.etudiantId, d.matiereId, valeur, LocalDate.now())), d.jeton, 201)
                .get("id").asText();
    }

    private String noterEtPublier(Decor d, String valeur) throws Exception {
        String id = saisirNote(d, valeur);
        publierLesNotes(d.jeton, d.classeId);
        return id;
    }

    private JsonNode appeler(MockHttpServletRequestBuilder requete, String jeton, int statutAttendu)
            throws Exception {
        var resultat = mockMvc.perform(avecJeton(requete, jeton)).andReturn();
        assertThat(resultat.getResponse().getStatus())
                .as("réponse : %s", resultat.getResponse().getContentAsString())
                .isEqualTo(statutAttendu);
        String corps = resultat.getResponse().getContentAsString();
        JsonNode data = corps.isBlank() ? null : objectMapper.readTree(corps).get("data");
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
