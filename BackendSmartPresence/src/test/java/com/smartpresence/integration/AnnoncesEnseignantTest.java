package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Annonces de l'administration et alertes automatiques sur les séances.
 */
@DisplayName("Intégration — annonces et notifications de l'enseignant")
class AnnoncesEnseignantTest extends IntegrationTestBase {

    @Test
    @DisplayName("une annonce de classe ne parvient qu'aux enseignants de cette classe")
    void annonceCiblee() throws Exception {
        Decor d = preparer("ANN-A");

        JsonNode nombre = appeler(post("/notifications/annonces").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("titre", "Réunion pédagogique", "message", "Jeudi 10h, salle des profs",
                        "classeId", d.classeId))), jetonAdmin, 201);
        assertThat(nombre.asInt()).isEqualTo(1);

        JsonNode recues = appeler(get("/notifications"), d.jeton, 200);
        assertThat(recues).hasSize(1);
        assertThat(recues.get(0).get("titre").asText()).isEqualTo("Réunion pédagogique");
        assertThat(recues.get(0).get("type").asText()).isEqualTo("ANNONCE");
        assertThat(recues.get(0).get("lue").asBoolean()).isFalse();

        assertThat(appeler(get("/notifications"), d.jetonAutre, 200))
                .as("l'enseignant d'une autre classe ne doit rien recevoir")
                .isEmpty();
        assertThat(appeler(get("/notifications/non-lues"), d.jeton, 200).asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("marquer lue : réservé au destinataire, et « tout lire » vide le compteur")
    void lecture() throws Exception {
        Decor d = preparer("ANN-B");
        for (String titre : new String[] {"Un", "Deux", "Trois"}) {
            appeler(post("/notifications/annonces").contentType(MediaType.APPLICATION_JSON)
                    .content(enJson(Map.of("titre", titre, "message", "Message", "personnelId", d.enseignantId))),
                    jetonAdmin, 201);
        }
        JsonNode recues = appeler(get("/notifications"), d.jeton, 200);
        assertThat(recues).hasSize(3);
        String id = recues.get(0).get("id").asText();

        appeler(patch("/notifications/" + id + "/lue"), d.jetonAutre, 403);
        assertThat(appeler(get("/notifications/non-lues"), d.jeton, 200).asInt()).isEqualTo(3);

        appeler(patch("/notifications/" + id + "/lue"), d.jeton, 200);
        assertThat(appeler(get("/notifications/non-lues"), d.jeton, 200).asInt()).isEqualTo(2);

        assertThat(appeler(patch("/notifications/lues"), d.jeton, 200).asInt()).isEqualTo(2);
        assertThat(appeler(get("/notifications/non-lues"), d.jeton, 200).asInt()).isZero();
    }

    @Test
    @DisplayName("seule l'administration diffuse, et une annonce vide est refusée")
    void diffusionReservee() throws Exception {
        Decor d = preparer("ANN-C");
        String corps = enJson(Map.of("titre", "Hors de propos", "message", "Message"));
        appeler(post("/notifications/annonces").contentType(MediaType.APPLICATION_JSON).content(corps), d.jeton, 403);
        appeler(post("/notifications/annonces").contentType(MediaType.APPLICATION_JSON)
                .content(enJson(Map.of("titre", " ", "message", "Message"))), jetonAdmin, 400);
    }

    @Test
    @DisplayName("déplacer, changer de salle ou annuler une séance prévient son enseignant")
    void alertesSurSeance() throws Exception {
        Decor d = preparer("ANN-D");
        Instant debut = Instant.now().plus(Duration.ofDays(2));
        Instant fin = debut.plus(Duration.ofHours(2));

        String seanceId = creer("/seances", """
                {"classeId":%d,"matiereId":%d,"enseignantId":"%s","debut":"%s","fin":"%s"}
                """.formatted(d.classeId, d.matiereId, d.enseignantId, debut, fin)).get("id").asText();
        JsonNode apresCreation = appeler(get("/notifications"), d.jeton, 200);
        assertThat(apresCreation).hasSize(1);
        assertThat(apresCreation.get(0).get("type").asText()).isEqualTo("SEANCE");

        long salleId = creer("/salles", """
                {"code":"S-ANN-D","nom":"Amphi D","capacite":80}
                """).get("id").asLong();
        appeler(put("/seances/" + seanceId).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"classeId":%d,"matiereId":%d,"enseignantId":"%s","salleId":%d,"debut":"%s","fin":"%s"}
                        """.formatted(d.classeId, d.matiereId, d.enseignantId, salleId,
                        debut.plus(Duration.ofHours(1)), fin.plus(Duration.ofHours(1)))), jetonAdmin, 200);

        JsonNode apresModification = appeler(get("/notifications"), d.jeton, 200);
        assertThat(apresModification).hasSize(2);
        JsonNode changement = apresModification.get(0);
        assertThat(changement.get("type").asText()).isEqualTo("CHANGEMENT");
        assertThat(changement.get("message").asText()).contains("Horaire").contains("Amphi D");

        // Une modification sans effet visible ne doit pas déranger.
        appeler(put("/seances/" + seanceId).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"classeId":%d,"matiereId":%d,"enseignantId":"%s","salleId":%d,"debut":"%s","fin":"%s","note":"RAS"}
                        """.formatted(d.classeId, d.matiereId, d.enseignantId, salleId,
                        debut.plus(Duration.ofHours(1)), fin.plus(Duration.ofHours(1)))), jetonAdmin, 200);
        assertThat(appeler(get("/notifications"), d.jeton, 200)).hasSize(2);

        appeler(patch("/seances/" + seanceId + "/statut").param("value", "ANNULEE"), jetonAdmin, 200);
        JsonNode apresAnnulation = appeler(get("/notifications"), d.jeton, 200);
        assertThat(apresAnnulation).hasSize(3);
        assertThat(apresAnnulation.get(0).get("titre").asText()).startsWith("Séance annulée");

        // Rappeler l'annulation ne renvoie pas une seconde alerte.
        appeler(patch("/seances/" + seanceId + "/statut").param("value", "ANNULEE"), jetonAdmin, 200);
        assertThat(appeler(get("/notifications"), d.jeton, 200)).hasSize(3);
    }

    // ------------------------------------------------------------------

    private record Decor(long classeId, long matiereId, String enseignantId, String jeton, String jetonAutre) { }

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
        String autreId = creer("/personnels", """
                {"matricule":"PROF-%s-2","nom":"Sow","prenom":"Modibo","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();

        mockMvc.perform(avecJeton(put("/classes/" + classeId), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","libelle":"Classe %s","promotionId":%d,"enseignantIds":["%s"]}
                                """.formatted(cle, cle, promotionId, enseignantId)))
                .andExpect(status().isOk());
        long matiereId = creer("/matieres", """
                {"code":"MAT-%s","libelle":"Matiere %s","credits":3}
                """.formatted(cle, cle)).get("id").asLong();

        return new Decor(classeId, matiereId, enseignantId,
                jetonDe("/personnels/" + enseignantId + "/compte"),
                jetonDe("/personnels/" + autreId + "/compte"));
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
