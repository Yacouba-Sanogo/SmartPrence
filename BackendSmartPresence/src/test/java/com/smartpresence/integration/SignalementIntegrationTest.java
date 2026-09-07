package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration du circuit de signalement.
 *
 * <p>Vérifient le principe : l'enseignant témoigne, la scolarité arbitre, et la
 * correction reste distinguable d'une identification biométrique.</p>
 */
@DisplayName("Intégration — signalement d'anomalie")
class SignalementIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("un signalement accepté produit un relevé de source MANUEL")
    void signalementAccepteProduitUnReleveManuel() throws Exception {
        Contexte c = preparer("SIG-A", "SIG-ET-A");

        MvcResult depot = mockMvc.perform(
                        avecJeton(post("/moi/seances/" + c.seanceId + "/signalements"), c.jetonEnseignant)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"type":"ETUDIANT_NON_RECONNU","etudiantId":"%s",
                                         "description":"Etudiant present, doigt non reconnu apres trois essais."}
                                        """.formatted(c.etudiantId)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode signalement = donnees(depot);
        assertThat(signalement.get("statut").asText()).isEqualTo("EN_ATTENTE");
        assertThat(signalement.get("presenceCorrectiveId").isNull())
                .as("aucune correction tant que la scolarite n'a pas tranche")
                .isTrue();

        // La scolarité arbitre.
        MvcResult arbitrage = mockMvc.perform(
                        avecJeton(patch("/signalements/" + signalement.get("id").asText() + "/traiter"),
                                jetonAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"accepte":true,"commentaire":"Confirme par le registre de la salle."}
                                        """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode traite = donnees(arbitrage);
        assertThat(traite.get("statut").asText()).isEqualTo("ACCEPTE");
        assertThat(traite.get("presenceCorrectiveId").isNull())
                .as("l'acceptation doit produire le releve correctif")
                .isFalse();

        // Le relevé apparaît désormais sur la feuille, marqué MANUEL.
        MvcResult feuille = mockMvc.perform(
                        avecJeton(get("/moi/seances/" + c.seanceId + "/feuille"), c.jetonEnseignant))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode ligne = donnees(feuille).get("lignes").get(0);
        assertThat(ligne.get("statut").asText()).isEqualTo("PRESENT");
        assertThat(ligne.get("source").asText())
                .as("une presence decidee par un humain ne doit jamais se confondre avec le capteur")
                .isEqualTo("MANUEL");
    }

    @Test
    @DisplayName("un signalement rejeté ne modifie aucun relevé")
    void signalementRejeteNeChangeRien() throws Exception {
        Contexte c = preparer("SIG-B", "SIG-ET-B");

        String signalementId = deposer(c, """
                {"type":"ETUDIANT_NON_RECONNU","etudiantId":"%s",
                 "description":"Absent selon moi, mais l'etudiant conteste."}
                """.formatted(c.etudiantId));

        mockMvc.perform(avecJeton(patch("/signalements/" + signalementId + "/traiter"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accepte":false,"commentaire":"Aucun element ne corrobore la presence."}
                                """))
                .andExpect(status().isOk());

        MvcResult feuille = mockMvc.perform(
                        avecJeton(get("/moi/seances/" + c.seanceId + "/feuille"), c.jetonEnseignant))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(donnees(feuille).get("absents").asInt()).isEqualTo(1);
        assertThat(donnees(feuille).get("lignes").get(0).get("statut").asText()).isEqualTo("ABSENT");
    }

    @Test
    @DisplayName("un signalement d'étudiant non reconnu exige de désigner l'étudiant")
    void etudiantObligatoirePourCeType() throws Exception {
        Contexte c = preparer("SIG-C", "SIG-ET-C");

        mockMvc.perform(avecJeton(post("/moi/seances/" + c.seanceId + "/signalements"), c.jetonEnseignant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ETUDIANT_NON_RECONNU","description":"Quelqu'un n'a pas ete lu."}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("une panne de lecteur se signale sans désigner personne")
    void panneDeLecteurNeVisePersonne() throws Exception {
        Contexte c = preparer("SIG-D", "SIG-ET-D");

        MvcResult depot = mockMvc.perform(
                        avecJeton(post("/moi/seances/" + c.seanceId + "/signalements"), c.jetonEnseignant)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"type":"LECTEUR_DEFAILLANT",
                                         "description":"Le lecteur de la salle est reste eteint toute la seance."}
                                        """))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(donnees(depot).get("etudiantId").isNull()).isTrue();
    }

    @Test
    @DisplayName("un arbitrage sans motif est refusé, dans les deux sens")
    void motifObligatoirePourArbitrer() throws Exception {
        Contexte c = preparer("SIG-E", "SIG-ET-E");
        String signalementId = deposer(c, """
                {"type":"LECTEUR_DEFAILLANT","description":"Lecteur hors service."}
                """);

        mockMvc.perform(avecJeton(patch("/signalements/" + signalementId + "/traiter"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accepte":true}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("un signalement déjà tranché ne peut pas être rejugé")
    void signalementDejaTraiteEstRefuse() throws Exception {
        Contexte c = preparer("SIG-F", "SIG-ET-F");
        String signalementId = deposer(c, """
                {"type":"LECTEUR_DEFAILLANT","description":"Lecteur hors service."}
                """);

        String corps = """
                {"accepte":false,"commentaire":"Deja verifie sur place."}
                """;
        mockMvc.perform(avecJeton(patch("/signalements/" + signalementId + "/traiter"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isOk());
        mockMvc.perform(avecJeton(patch("/signalements/" + signalementId + "/traiter"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("un enseignant ne peut pas arbitrer ses propres signalements")
    void enseignantNArbitrePas() throws Exception {
        Contexte c = preparer("SIG-G", "SIG-ET-G");
        String signalementId = deposer(c, """
                {"type":"LECTEUR_DEFAILLANT","description":"Lecteur hors service."}
                """);

        mockMvc.perform(avecJeton(patch("/signalements/" + signalementId + "/traiter"), c.jetonEnseignant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accepte":true,"commentaire":"Je confirme moi-meme."}
                                """))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------

    private record Contexte(String seanceId, String etudiantId, String jetonEnseignant) {}

    /** Monte une promotion, une classe, un enseignant, un étudiant et une séance. */
    private Contexte preparer(String cle, String matriculeEtudiant) throws Exception {
        long promotionId = creer("/promotions", """
                {"code":"PROMO-%s","libelle":"Promotion %s","anneeAcademique":2026}
                """.formatted(cle, cle)).get("id").asLong();

        long classeId = creer("/classes", """
                {"code":"%s","libelle":"Classe %s","promotionId":%d}
                """.formatted(cle, cle, promotionId)).get("id").asLong();

        String enseignantId = creer("/personnels", """
                {"matricule":"PROF-%s","nom":"Kone","prenom":"Ali","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();

        mockMvc.perform(avecJeton(put("/classes/" + classeId), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","libelle":"Classe %s","promotionId":%d,"enseignantIds":["%s"]}
                                """.formatted(cle, cle, promotionId, enseignantId)))
                .andExpect(status().isOk());

        String etudiantId = creer("/etudiants", """
                {"matricule":"%s","nom":"Diarra","prenom":"Ami","classeId":%d,"actif":true}
                """.formatted(matriculeEtudiant, classeId)).get("id").asText();

        long matiereId = creer("/matieres", """
                {"code":"MAT-%s","libelle":"Matiere %s","credits":3}
                """.formatted(cle, cle)).get("id").asLong();

        Instant debut = LocalDate.now().atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant();
        Instant fin = LocalDate.now().atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant();
        String seanceId = creer("/seances", """
                {"classeId":%d,"matiereId":%d,"enseignantId":"%s","debut":"%s","fin":"%s"}
                """.formatted(classeId, matiereId, enseignantId, debut, fin)).get("id").asText();

        JsonNode compte = creer("/personnels/" + enseignantId + "/compte", null);
        String jeton = seConnecter(
                compte.get("email").asText(), compte.get("motDePasseInitial").asText());

        return new Contexte(seanceId, etudiantId, jeton);
    }

    private JsonNode creer(String chemin, String corps) throws Exception {
        var requete = avecJeton(post(chemin), jetonAdmin);
        if (corps != null) {
            requete = requete.contentType(MediaType.APPLICATION_JSON).content(corps);
        }
        return donnees(mockMvc.perform(requete).andExpect(status().isCreated()).andReturn());
    }

    private String deposer(Contexte c, String corps) throws Exception {
        MvcResult r = mockMvc.perform(
                        avecJeton(post("/moi/seances/" + c.seanceId + "/signalements"), c.jetonEnseignant)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corps))
                .andExpect(status().isCreated())
                .andReturn();
        return donnees(r).get("id").asText();
    }
}
