package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration du parcours complet de pointage biométrique du personnel.
 *
 * <p>Déroule la chaîne réelle : création de l'agent, enrôlement, déclaration du
 * lecteur, envoi d'un lot par l'ESP32 authentifié par clé d'API, puis restitution
 * de la synthèse journalière — le tout en base MySQL.</p>
 */
@DisplayName("Intégration — parcours de pointage du personnel")
class PointagePersonnelIntegrationTest extends IntegrationTestBase {

    private static final String BIOMETRIC_ID = "PER-0042";

    /**
     * Chaque test déclare son propre lecteur avec sa propre clé d'API.
     *
     * <p>Les tests partagent le même contexte Spring et le même schéma : des lecteurs
     * porteurs d'une clé identique se disputeraient la résolution par clé, qui retient
     * la première correspondance trouvée. Une clé par appareil est de toute façon la
     * règle en exploitation — elle seule permet de révoquer un dispositif isolément.</p>
     */
    private static String cleApiPour(String nomLecteur) {
        return "cle-test-" + nomLecteur.toLowerCase();
    }

    @Test
    @DisplayName("de l'enrôlement à la synthèse : le parcours complet aboutit")
    void parcoursCompletDePointage() throws Exception {
        LocalDate jour = LocalDate.now();

        String agentId = creerAgent("AG-001", "Traore", "Aminata", "ADMINISTRATIF", "Scolarite");
        enroler(agentId, BIOMETRIC_ID);
        String deviceId = creerLecteur("ESP32-Entree-Principale", "AA:BB:CC:DD:EE:01", "PERSONNEL");

        // Le lot est volontairement transmis À L'ENVERS : une file NVS vidée après
        // une coupure réseau n'arrive pas nécessairement dans l'ordre, et le sens
        // entrée/sortie serait alors inversé.
        JsonNode resume = synchroniser("ESP32-Entree-Principale", deviceId, """
                [
                  {"biometricId":"%s","deviceId":"%s","date":"%s","heure":"17:05:00","createdAt":"%sT17:05:00Z"},
                  {"biometricId":"%s","deviceId":"%s","date":"%s","heure":"08:31:00","createdAt":"%sT08:31:00Z"}
                ]
                """.formatted(BIOMETRIC_ID, deviceId, jour, jour, BIOMETRIC_ID, deviceId, jour, jour));

        assertThat(resume.get("totalInseres").asInt()).isEqualTo(2);
        assertThat(resume.get("totalIgnoresDoublons").asInt()).isZero();

        JsonNode journee = journeeDeLAgent(jour, "AG-001");
        assertThat(journee.get("heureEntree").asText())
                .as("le pointage le plus matinal doit être retenu comme l'entrée")
                .isEqualTo("08:31:00");
        assertThat(journee.get("heureSortie").asText()).isEqualTo("17:05:00");
        assertThat(journee.get("statut").asText())
                .as("08:31 dépasse 08:00 + 15 min de tolérance")
                .isEqualTo("RETARD");
        assertThat(journee.get("minutesRetard").asLong()).isEqualTo(31);
        assertThat(journee.get("minutesTravaillees").asLong()).isEqualTo(514);
        assertThat(journee.get("present").asBoolean())
                .as("la sortie étant pointée, l'agent n'est plus sur site")
                .isFalse();
    }

    @Test
    @DisplayName("un lot retransmis ne crée aucun doublon")
    void retransmissionEstIdempotente() throws Exception {
        LocalDate jour = LocalDate.now();
        String agentId = creerAgent("AG-002", "Coulibaly", "Moussa", "TECHNIQUE", "Maintenance");
        enroler(agentId, "PER-0043");
        String deviceId = creerLecteur("ESP32-Entree-Est", "AA:BB:CC:DD:EE:02", "PERSONNEL");

        String lot = """
                [{"biometricId":"PER-0043","deviceId":"%s","date":"%s","heure":"08:04:00","createdAt":"%sT08:04:00Z"}]
                """.formatted(deviceId, jour, jour);

        assertThat(synchroniser("ESP32-Entree-Est", deviceId, lot).get("totalInseres").asInt()).isEqualTo(1);

        JsonNode secondEnvoi = synchroniser("ESP32-Entree-Est", deviceId, lot);
        assertThat(secondEnvoi.get("totalInseres").asInt()).isZero();
        assertThat(secondEnvoi.get("totalIgnoresDoublons").asInt()).isEqualTo(1);

        assertThat(journeeDeLAgent(jour, "AG-002").get("nombrePointages").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("une arrivée dans la tolérance reste PRESENT malgré un retard mesuré")
    void arriveeDansLaToleranceResteAlHeure() throws Exception {
        LocalDate jour = LocalDate.now();
        String agentId = creerAgent("AG-003", "Sidibe", "Oumar", "TECHNIQUE", "Informatique");
        enroler(agentId, "PER-0044");
        String deviceId = creerLecteur("ESP32-Entree-Ouest", "AA:BB:CC:DD:EE:03", "PERSONNEL");

        synchroniser("ESP32-Entree-Ouest", deviceId, """
                [{"biometricId":"PER-0044","deviceId":"%s","date":"%s","heure":"08:04:00","createdAt":"%sT08:04:00Z"}]
                """.formatted(deviceId, jour, jour));

        JsonNode journee = journeeDeLAgent(jour, "AG-003");
        assertThat(journee.get("statut").asText()).isEqualTo("PRESENT");
        assertThat(journee.get("minutesRetard").asLong())
                .as("le retard est mesuré depuis l'ouverture, indépendamment de la tolérance")
                .isEqualTo(4);
        assertThat(journee.get("present").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("un lecteur de salle est refusé sur le flux du personnel")
    void lecteurEtudiantRefuseSurLeFluxPersonnel() throws Exception {
        LocalDate jour = LocalDate.now();
        String agentId = creerAgent("AG-004", "Cisse", "Mariam", "ADMINISTRATIF", "Comptabilite");
        enroler(agentId, "PER-0045");
        String deviceId = creerLecteur("ESP32-Salle-B12", "AA:BB:CC:DD:EE:04", "ETUDIANT");

        mockMvc.perform(post("/esp32/personnels/pointages")
                        .header("X-API-KEY", cleApiPour("ESP32-Salle-B12"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":"%s","nombreTentatives":1,"pointages":
                                 [{"biometricId":"PER-0045","deviceId":"%s","date":"%s","heure":"08:00:00","createdAt":"%sT08:00:00Z"}]}
                                """.formatted(deviceId, deviceId, jour, jour)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deux appareils ne peuvent pas partager la même clé d'API")
    void cleApiEstUniqueParAppareil() throws Exception {
        creerLecteur("ESP32-Entree-Sud", "AA:BB:CC:DD:EE:06", "PERSONNEL");

        // Même clé, autre adresse MAC : le doublon doit être refusé.
        // Le contrôle d'unicité était auparavant inopérant — l'empreinte BCrypt
        // différait à chaque calcul, aucun doublon n'était donc jamais détecté.
        mockMvc.perform(avecJeton(post("/devices"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"ESP32-Clone","adresseMac":"AA:BB:CC:DD:EE:07","apiKey":"%s","usage":"PERSONNEL"}
                                """.formatted(cleApiPour("ESP32-Entree-Sud"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("une clé d'API trop courte est refusée")
    void cleApiTropCourteEstRefusee() throws Exception {
        mockMvc.perform(avecJeton(post("/devices"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"ESP32-Faible","adresseMac":"AA:BB:CC:DD:EE:08","apiKey":"123","usage":"PERSONNEL"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("une clé d'API inconnue est rejetée")
    void cleApiInconnueEstRejetee() throws Exception {
        LocalDate jour = LocalDate.now();
        mockMvc.perform(post("/esp32/personnels/pointages")
                        .header("X-API-KEY", "cle-invalide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":"00000000-0000-0000-0000-000000000000","pointages":
                                 [{"biometricId":"PER-0000","deviceId":"00000000-0000-0000-0000-000000000000",
                                   "date":"%s","heure":"08:00:00","createdAt":"%sT08:00:00Z"}]}
                                """.formatted(jour, jour)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un agent non enrôlé ne peut pas être identifié")
    void agentNonEnroleNEstPasIdentifie() throws Exception {
        LocalDate jour = LocalDate.now();
        creerAgent("AG-005", "Sangare", "Kadidia", "ENSEIGNANT", "Informatique");
        String deviceId = creerLecteur("ESP32-Entree-Nord", "AA:BB:CC:DD:EE:05", "PERSONNEL");

        // L'agent existe mais n'a aucune référence biométrique : le lot est accepté,
        // l'événement est écarté et signalé.
        JsonNode resume = synchroniser("ESP32-Entree-Nord", deviceId, """
                [{"biometricId":"PER-INEXISTANT","deviceId":"%s","date":"%s","heure":"08:00:00","createdAt":"%sT08:00:00Z"}]
                """.formatted(deviceId, jour, jour));

        assertThat(resume.get("totalInseres").asInt()).isZero();
        assertThat(resume.get("totalTraites").asInt()).isEqualTo(1);

        assertThat(journeeDeLAgent(jour, "AG-005").get("statut").asText()).isEqualTo("ABSENT");
    }

    @Test
    @DisplayName("une régularisation manuelle exige un motif et reste distinguable")
    void regularisationManuelleExigeUnMotif() throws Exception {
        LocalDate jour = LocalDate.now();
        String agentId = creerAgent("AG-006", "Dembele", "Seydou", "SECURITE", "Surete");

        // Sans motif, la requête est rejetée : une correction non justifiée
        // ruinerait la valeur probante du relevé.
        mockMvc.perform(avecJeton(post("/pointages/manuel"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"personnelId":"%s","datePointage":"%s","heurePointage":"06:58:00","sens":"ENTREE"}
                                """.formatted(agentId, jour)))
                .andExpect(status().isBadRequest());

        MvcResult resultat = mockMvc.perform(avecJeton(post("/pointages/manuel"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"personnelId":"%s","datePointage":"%s","heurePointage":"06:58:00","sens":"ENTREE",
                                 "motif":"Capteur du hall A hors service — arrivee confirmee par la securite."}
                                """.formatted(agentId, jour)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode pointage = donnees(resultat);
        assertThat(pointage.get("source").asText())
                .as("la source MANUEL distingue la correction d'une identification biométrique")
                .isEqualTo("MANUEL");
        assertThat(pointage.get("motif").asText()).contains("hors service");
    }

    // ------------------------------------------------------------------
    // Étapes réutilisables du parcours
    // ------------------------------------------------------------------

    private String creerAgent(String matricule, String nom, String prenom, String type, String service)
            throws Exception {
        MvcResult resultat = mockMvc.perform(avecJeton(post("/personnels"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matricule":"%s","nom":"%s","prenom":"%s","type":"%s","service":"%s","actif":true}
                                """.formatted(matricule, nom, prenom, type, service)))
                .andExpect(status().isCreated())
                .andReturn();
        return donnees(resultat).get("id").asText();
    }

    private void enroler(String agentId, String biometricId) throws Exception {
        MvcResult resultat = mockMvc.perform(avecJeton(post("/personnels/" + agentId + "/enrolement"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"biometricId":"%s"}
                                """.formatted(biometricId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode agent = donnees(resultat);
        assertThat(agent.get("enrole").asBoolean()).isTrue();
        assertThat(agent.get("biometricId").asText()).isEqualTo(biometricId);
    }

    private String creerLecteur(String nom, String adresseMac, String usage) throws Exception {
        MvcResult resultat = mockMvc.perform(avecJeton(post("/devices"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"%s","adresseMac":"%s","apiKey":"%s","usage":"%s"}
                                """.formatted(nom, adresseMac, cleApiPour(nom), usage)))
                .andExpect(status().isCreated())
                .andReturn();
        return donnees(resultat).get("id").asText();
    }

    private JsonNode synchroniser(String nomLecteur, String deviceId, String pointagesJson) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/esp32/personnels/pointages")
                        .header("X-API-KEY", cleApiPour(nomLecteur))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":"%s","nombreTentatives":1,"pointages":%s}
                                """.formatted(deviceId, pointagesJson)))
                .andExpect(status().isOk())
                .andReturn();
        return donnees(resultat);
    }

    /** Retrouve la ligne de synthèse d'un agent dans la journée. */
    private JsonNode journeeDeLAgent(LocalDate jour, String matricule) throws Exception {
        MvcResult resultat = mockMvc.perform(
                        avecJeton(get("/pointages/journee").param("date", jour.toString()), jetonAdmin))
                .andExpect(status().isOk())
                .andReturn();

        for (JsonNode ligne : donnees(resultat)) {
            if (matricule.equals(ligne.get("matricule").asText())) {
                return ligne;
            }
        }
        throw new AssertionError("Agent " + matricule + " absent de la synthèse du " + jour);
    }
}
