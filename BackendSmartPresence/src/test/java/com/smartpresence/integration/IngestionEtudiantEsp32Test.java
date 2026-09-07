package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ingestion des présences étudiantes remontées par un lecteur ESP32.
 *
 * <p>Ces tests couvrent le maillon qui manquait : le lecteur ne connaît pas l'emploi du
 * temps, il ne transmet qu'un étudiant et une heure. Si le serveur ne retrouve pas la
 * séance, le relevé reste orphelin — et la feuille de présence de l'enseignant affiche
 * toute la classe absente alors même que le capteur a identifié les étudiants.</p>
 */
@DisplayName("Intégration — ingestion des présences étudiantes")
class IngestionEtudiantEsp32Test extends IntegrationTestBase {

    private static final String CLE_API = "TEST-ESP32-ETUDIANTS-0001";

    @Test
    @DisplayName("un relevé pendant le cours est rattaché à la séance")
    void releveRattacheALaSeance() throws Exception {
        Decor d = preparer("ING-A");

        synchroniser(d, d.biometricId, LocalTime.of(8, 5)).andExpect(status().isOk());

        JsonNode releve = premierReleve(d);
        assertThat(releve.get("seanceId").asText())
                .as("sans rattachement, la feuille de l'enseignant reste vide")
                .isEqualTo(d.seanceId);
        assertThat(releve.get("matiereLibelle").asText()).isNotBlank();
        assertThat(releve.get("source").asText()).isEqualTo("ESP32");
    }

    @Test
    @DisplayName("le relevé rattaché apparaît sur la feuille de l'enseignant")
    void feuilleDeSeanceRefleteLeReleve() throws Exception {
        Decor d = preparer("ING-B");
        synchroniser(d, d.biometricId, LocalTime.of(8, 5)).andExpect(status().isOk());

        JsonNode feuille = donnees(mockMvc.perform(
                        avecJeton(get("/moi/seances/" + d.seanceId + "/feuille"), d.jetonEnseignant))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(feuille.get("presents").asInt()).isEqualTo(1);
        assertThat(feuille.get("absents").asInt()).isZero();
        assertThat(feuille.get("lignes").get(0).get("heure").asText()).startsWith("08:05");
    }

    @Test
    @DisplayName("un étudiant qui badge un peu avant le cours est rattaché quand même")
    void arriveeAnticipeeToleree() throws Exception {
        Decor d = preparer("ING-C");

        // Badger en arrivant, dix minutes avant l'heure, est le cas courant.
        synchroniser(d, d.biometricId, LocalTime.of(7, 50)).andExpect(status().isOk());

        assertThat(premierReleve(d).get("seanceId").asText()).isEqualTo(d.seanceId);
    }

    @Test
    @DisplayName("un relevé hors de tout cours reste enregistré, sans séance")
    void releveHorsCoursResteOrphelin() throws Exception {
        Decor d = preparer("ING-D");

        // 18 h : aucune séance. Le passage reste un fait, et doit être conservé.
        synchroniser(d, d.biometricId, LocalTime.of(18, 0)).andExpect(status().isOk());

        JsonNode releve = premierReleve(d);
        assertThat(releve.get("seanceId").isNull()).isTrue();
        assertThat(releve.get("statut").asText()).isEqualTo("PRESENT");
    }

    @Test
    @DisplayName("le rejeu d'un même lot n'insère aucun doublon")
    void rejeuIdempotent() throws Exception {
        Decor d = preparer("ING-E");

        JsonNode premier = donnees(synchroniser(d, d.biometricId, LocalTime.of(8, 5))
                .andExpect(status().isOk()).andReturn());
        assertThat(premier.get("totalInseres").asInt()).isEqualTo(1);

        // La file NVS du lecteur peut renvoyer un lot déjà transmis après une coupure.
        JsonNode second = donnees(synchroniser(d, d.biometricId, LocalTime.of(8, 5))
                .andExpect(status().isOk()).andReturn());
        assertThat(second.get("totalInseres").asInt()).isZero();
        assertThat(second.get("totalIgnoresDoublons").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("une séance annulée ne capte pas les relevés")
    void seanceAnnuleeNeCaptePas() throws Exception {
        Decor d = preparer("ING-F");

        mockMvc.perform(avecJeton(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/seances/" + d.seanceId + "/statut")
                                .param("value", "ANNULEE"), jetonAdmin))
                .andExpect(status().isOk());

        synchroniser(d, d.biometricId, LocalTime.of(8, 5)).andExpect(status().isOk());

        assertThat(premierReleve(d).get("seanceId").isNull())
                .as("un cours annule n'a pas eu lieu : lui rattacher des presences serait faux")
                .isTrue();
    }

    @Test
    @DisplayName("une clé d'API inconnue est refusée")
    void cleApiInconnue() throws Exception {
        Decor d = preparer("ING-G");

        // 403 et non 401 : le filtre n'authentifie pas la clé inconnue, la requête
        // poursuit sans identité, et c'est l'autorisation hasRole('DEVICE') qui refuse.
        mockMvc.perform(post("/esp32/presences/synchronize")
                        .header("X-API-KEY", "CLE-QUI-N-EXISTE-PAS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lot(d, d.biometricId, LocalTime.of(8, 5))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------

    private record Decor(String deviceId, String cleApi, String seanceId,
                         String biometricId, long classeId,
                         String jetonEnseignant) {}

    private org.springframework.test.web.servlet.ResultActions synchroniser(
            Decor d, String biometricId, LocalTime heure) throws Exception {
        return mockMvc.perform(post("/esp32/presences/synchronize")
                .header("X-API-KEY", d.cleApi)
                .contentType(MediaType.APPLICATION_JSON)
                .content(lot(d, biometricId, heure)));
    }

    private String lot(Decor d, String biometricId, LocalTime heure) {
        Instant horodatage = LocalDate.now().atTime(heure)
                .atZone(ZoneId.systemDefault()).toInstant();
        // Le contrat impose HH:mm:ss ; LocalTime.toString() omet les secondes a zero.
        return """
                {"deviceId":"%s","nombreTentatives":1,
                 "presences":[{"biometricId":"%s","deviceId":"%s","date":"%s","heure":"%s",
                               "statut":"PRESENT","createdAt":"%s"}]}
                """.formatted(d.deviceId, biometricId, d.deviceId,
                LocalDate.now(), heure.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                horodatage);
    }

    private JsonNode premierReleve(Decor d) throws Exception {
        MvcResult resultat = mockMvc.perform(avecJeton(get("/presences")
                        .param("classeId", String.valueOf(d.classeId)), jetonAdmin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode contenu = donnees(resultat).get("content");
        assertThat(contenu).as("aucun releve enregistre").isNotEmpty();
        return contenu.get(0);
    }

    private Decor preparer(String cle) throws Exception {
        long promotionId = creer("/promotions", """
                {"code":"PROMO-%s","libelle":"Promotion %s","anneeAcademique":2026}
                """.formatted(cle, cle)).get("id").asLong();

        long classeId = creer("/classes", """
                {"code":"%s","libelle":"Classe %s","promotionId":%d}
                """.formatted(cle, cle, promotionId)).get("id").asLong();

        String enseignantId = creer("/personnels", """
                {"matricule":"PROF-%s","nom":"Cisse","prenom":"Oumar","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();

        mockMvc.perform(avecJeton(put("/classes/" + classeId), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","libelle":"Classe %s","promotionId":%d,"enseignantIds":["%s"]}
                                """.formatted(cle, cle, promotionId, enseignantId)))
                .andExpect(status().isOk());

        String etudiantId = creer("/etudiants", """
                {"matricule":"%s-ET1","nom":"Keita","prenom":"Sira","classeId":%d,"actif":true}
                """.formatted(cle, classeId)).get("id").asText();

        // Sans empreinte associee, le lecteur ne pourrait pas identifier l'etudiant.
        mockMvc.perform(avecJeton(post("/etudiants/" + etudiantId + "/enrolement"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"biometricId":"BIO-%s"}
                                """.formatted(cle)))
                .andExpect(status().isOk());

        long matiereId = creer("/matieres", """
                {"code":"MAT-%s","libelle":"Matiere %s","credits":3}
                """.formatted(cle, cle)).get("id").asLong();

        long salleId = creer("/salles", """
                {"code":"%s-S","nom":"Salle %s","capacite":40}
                """.formatted(cle, cle)).get("id").asLong();

        ZoneId zone = ZoneId.systemDefault();
        String seanceId = creer("/seances", """
                {"classeId":%d,"matiereId":%d,"enseignantId":"%s","salleId":%d,
                 "debut":"%s","fin":"%s"}
                """.formatted(classeId, matiereId, enseignantId, salleId,
                LocalDate.now().atTime(8, 0).atZone(zone).toInstant(),
                LocalDate.now().atTime(10, 0).atZone(zone).toInstant())).get("id").asText();

        String cleApi = CLE_API + "-" + cle;
        // L'adresse MAC est unique : elle derive de la cle du decor.
        String mac = "AA:BB:CC:00:%02d:%02d".formatted(
                Math.abs(cle.hashCode()) % 100, Math.abs(cle.hashCode() / 100) % 100);
        String deviceId = creer("/devices", """
                {"nom":"Lecteur %s","adresseMac":"%s","apiKey":"%s",
                 "statut":"ACTIF","usage":"MIXTE","salleId":%d}
                """.formatted(cle, mac, cleApi, salleId))
                .get("id").asText();

        JsonNode compte = creer("/personnels/" + enseignantId + "/compte", null);
        String jeton = seConnecter(
                compte.get("email").asText(), compte.get("motDePasseInitial").asText());

        // Le lecteur ne connait que la reference logique de l'empreinte.
        return new Decor(deviceId, cleApi, seanceId, "BIO-" + cle, classeId, jeton);
    }

    private JsonNode creer(String chemin, String corps) throws Exception {
        var requete = avecJeton(post(chemin), jetonAdmin);
        if (corps != null) {
            requete = requete.contentType(MediaType.APPLICATION_JSON).content(corps);
        }
        return donnees(mockMvc.perform(requete).andExpect(status().isCreated()).andReturn());
    }
}
