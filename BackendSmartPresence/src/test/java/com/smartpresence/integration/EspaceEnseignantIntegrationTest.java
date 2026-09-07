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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration de l'espace enseignant.
 *
 * <p>Vérifient le cadrage : un enseignant ne voit que ses classes, ses séances, et les
 * feuilles des cours qu'il assure.</p>
 */
@DisplayName("Intégration — espace enseignant")
class EspaceEnseignantIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("un enseignant ne voit que ses propres classes")
    void enseignantNeVoitQueSesClasses() throws Exception {
        long promotionId = creerPromotion("PROMO-ENS", "Promotion enseignant");
        long maClasse = creerClasse("ENS-A", "Classe A", promotionId);
        long autreClasse = creerClasse("ENS-B", "Classe B", promotionId);

        String monId = creerEnseignant("PROF-001", "Traore", "Salif");
        String autreId = creerEnseignant("PROF-002", "Diallo", "Fanta");

        rattacher(maClasse, "ENS-A", "Classe A", promotionId, monId);
        rattacher(autreClasse, "ENS-B", "Classe B", promotionId, autreId);

        String jeton = ouvrirEtConnecter(monId);

        MvcResult resultat = mockMvc.perform(avecJeton(get("/moi/classes"), jeton))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode classes = donnees(resultat);
        assertThat(classes).hasSize(1);
        assertThat(classes.get(0).get("code").asText())
                .as("la classe d'un collègue ne doit pas remonter")
                .isEqualTo("ENS-A");
    }

    @Test
    @DisplayName("la feuille d'une séance recense TOUT l'effectif, pas seulement les relevés")
    void feuilleRecenseToutLEffectif() throws Exception {
        long promotionId = creerPromotion("PROMO-FEU", "Promotion feuille");
        long classeId = creerClasse("FEU-1", "Classe feuille", promotionId);
        String enseignantId = creerEnseignant("PROF-010", "Kone", "Adama");
        rattacher(classeId, "FEU-1", "Classe feuille", promotionId, enseignantId);

        // Deux étudiants : l'un enrôlé, l'autre non.
        creerEtudiant("FEU-ET-1", "Sidibe", "Oumar", classeId, "ETU-9001");
        creerEtudiant("FEU-ET-2", "Coulibaly", "Awa", classeId, null);

        long matiereId = creerMatiere("MAT-FEU", "Algorithmique");
        String seanceId = creerSeance(classeId, matiereId, enseignantId);

        String jeton = ouvrirEtConnecter(enseignantId);
        MvcResult resultat = mockMvc.perform(
                        avecJeton(get("/moi/seances/" + seanceId + "/feuille"), jeton))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode feuille = donnees(resultat);
        assertThat(feuille.get("effectif").asInt())
                .as("un étudiant jamais identifié doit tout de même figurer sur la feuille")
                .isEqualTo(2);
        assertThat(feuille.get("absents").asInt()).isEqualTo(2);
        assertThat(feuille.get("absentsNonEnroles").asInt())
                .as("l'absence d'un non-enrôlé ne dit rien de son assiduité : elle est comptée à part")
                .isEqualTo(1);

        JsonNode lignes = feuille.get("lignes");
        assertThat(lignes).hasSize(2);
        for (JsonNode ligne : lignes) {
            assertThat(ligne.get("statut").asText()).isEqualTo("ABSENT");
            assertThat(ligne.get("heure").isNull()).isTrue();
        }
    }

    @Test
    @DisplayName("un enseignant ne peut pas ouvrir la feuille du cours d'un collègue")
    void feuilleDunCollegueEstRefusee() throws Exception {
        long promotionId = creerPromotion("PROMO-CLO", "Promotion cloisonnement");
        long classeId = creerClasse("CLO-1", "Classe cloisonnee", promotionId);
        String proprietaire = creerEnseignant("PROF-020", "Sanogo", "Moussa");
        String intrus = creerEnseignant("PROF-021", "Berthe", "Nana");
        rattacher(classeId, "CLO-1", "Classe cloisonnee", promotionId, proprietaire);

        long matiereId = creerMatiere("MAT-CLO", "Reseaux");
        String seanceId = creerSeance(classeId, matiereId, proprietaire);

        String jetonIntrus = ouvrirEtConnecter(intrus);
        mockMvc.perform(avecJeton(get("/moi/seances/" + seanceId + "/feuille"), jetonIntrus))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un agent non enseignant est refusé sur l'espace enseignant")
    void agentNonEnseignantEstRefuse() throws Exception {
        String agentId = creerAgent("ADM-001", "Cisse", "Mariam", "ADMINISTRATIF");
        String jeton = ouvrirEtConnecter(agentId);

        mockMvc.perform(avecJeton(get("/moi/classes"), jeton)).andExpect(status().isForbidden());
        mockMvc.perform(avecJeton(get("/moi/seances"), jeton)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("le profil d'un agent de catégorie enseignant se résout en ENSEIGNANT")
    void profilAgentEnseignant() throws Exception {
        String enseignantId = creerEnseignant("PROF-030", "Toure", "Ibrahim");
        String jeton = ouvrirEtConnecter(enseignantId);

        MvcResult resultat = mockMvc.perform(avecJeton(get("/moi/profil"), jeton))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode profil = donnees(resultat);
        assertThat(profil.get("typeProfil").asText())
                .as("une seule fiche Personnel porte désormais les deux profils métier")
                .isEqualTo("ENSEIGNANT");
        assertThat(profil.get("matricule").asText()).isEqualTo("PROF-030");
    }

    // ------------------------------------------------------------------
    // Fabriques
    // ------------------------------------------------------------------

    private long creerPromotion(String code, String libelle) throws Exception {
        MvcResult r = mockMvc.perform(avecJeton(post("/promotions"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","libelle":"%s","anneeAcademique":2026}
                                """.formatted(code, libelle)))
                .andExpect(status().isCreated())
                .andReturn();
        return donnees(r).get("id").asLong();
    }

    private long creerClasse(String code, String libelle, long promotionId) throws Exception {
        MvcResult r = mockMvc.perform(avecJeton(post("/classes"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","libelle":"%s","promotionId":%d}
                                """.formatted(code, libelle, promotionId)))
                .andExpect(status().isCreated())
                .andReturn();
        return donnees(r).get("id").asLong();
    }

    /** Rattache un enseignant à une classe via la mise à jour de celle-ci. */
    private void rattacher(long classeId, String code, String libelle, long promotionId,
                           String enseignantId) throws Exception {
        mockMvc.perform(avecJeton(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/classes/" + classeId), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","libelle":"%s","promotionId":%d,"enseignantIds":["%s"]}
                                """.formatted(code, libelle, promotionId, enseignantId)))
                .andExpect(status().isOk());
    }

    private String creerEnseignant(String matricule, String nom, String prenom) throws Exception {
        return creerAgent(matricule, nom, prenom, "ENSEIGNANT");
    }

    private String creerAgent(String matricule, String nom, String prenom, String type)
            throws Exception {
        MvcResult r = mockMvc.perform(avecJeton(post("/personnels"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matricule":"%s","nom":"%s","prenom":"%s","type":"%s","actif":true}
                                """.formatted(matricule, nom, prenom, type)))
                .andExpect(status().isCreated())
                .andReturn();
        return donnees(r).get("id").asText();
    }

    private void creerEtudiant(String matricule, String nom, String prenom, long classeId,
                               String biometricId) throws Exception {
        String corps = biometricId == null
                ? """
                  {"matricule":"%s","nom":"%s","prenom":"%s","classeId":%d,"actif":true}
                  """.formatted(matricule, nom, prenom, classeId)
                : """
                  {"matricule":"%s","nom":"%s","prenom":"%s","classeId":%d,"actif":true,"biometricId":"%s"}
                  """.formatted(matricule, nom, prenom, classeId, biometricId);

        mockMvc.perform(avecJeton(post("/etudiants"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated());
    }

    private long creerMatiere(String code, String libelle) throws Exception {
        MvcResult r = mockMvc.perform(avecJeton(post("/matieres"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","libelle":"%s","credits":3}
                                """.formatted(code, libelle)))
                .andExpect(status().isCreated())
                .andReturn();
        return donnees(r).get("id").asLong();
    }

    private String creerSeance(long classeId, long matiereId, String enseignantId) throws Exception {
        Instant debut = LocalDate.now().atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant();
        Instant fin = LocalDate.now().atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant();

        MvcResult r = mockMvc.perform(avecJeton(post("/seances"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"classeId":%d,"matiereId":%d,"enseignantId":"%s","debut":"%s","fin":"%s"}
                                """.formatted(classeId, matiereId, enseignantId, debut, fin)))
                .andExpect(status().isCreated())
                .andReturn();
        return donnees(r).get("id").asText();
    }

    /** Ouvre un compte pour un agent puis retourne son jeton. */
    private String ouvrirEtConnecter(String personnelId) throws Exception {
        MvcResult r = mockMvc.perform(avecJeton(post("/personnels/" + personnelId + "/compte"), jetonAdmin))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode compte = donnees(r);
        return seConnecter(compte.get("email").asText(), compte.get("motDePasseInitial").asText());
    }
}
