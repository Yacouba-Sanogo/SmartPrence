package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration des comptes étudiants et du cloisonnement des données.
 *
 * <p>Ces tests couvrent le prérequis du volet mobile : un étudiant doit pouvoir se
 * connecter, et ne voir <b>que</b> son propre dossier.</p>
 */
@DisplayName("Intégration — comptes étudiants et cloisonnement")
class CompteEtudiantIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("un étudiant reçoit un accès mobile et peut se connecter")
    void etudiantRecoitUnAccesEtSeConnecte() throws Exception {
        String etudiantId = creerEtudiant("ET-001", "Sangare", "Kadidia", "BIO-1001");

        MvcResult creation = mockMvc.perform(avecJeton(post("/etudiants/" + etudiantId + "/compte"), jetonAdmin))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode compte = donnees(creation);
        String email = compte.get("email").asText();
        String motDePasse = compte.get("motDePasseInitial").asText();

        assertThat(compte.get("matricule").asText()).isEqualTo("ET-001");
        assertThat(motDePasse)
                .as("le mot de passe initial doit être tiré au hasard, jamais dérivé du matricule")
                .isNotBlank()
                .doesNotContain("ET-001");

        // Le compte doit être immédiatement utilisable et porter le rôle ETUDIANT.
        String jetonEtudiant = seConnecter(email, motDePasse);
        MvcResult profil = mockMvc.perform(avecJeton(get("/moi/profil"), jetonEtudiant))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode identite = donnees(profil);
        assertThat(identite.get("typeProfil").asText()).isEqualTo("ETUDIANT");
        assertThat(identite.get("matricule").asText()).isEqualTo("ET-001");
        assertThat(identite.get("roles").toString()).contains("ETUDIANT");
        assertThat(identite.get("classeCode")).isNotNull();
    }

    @Test
    @DisplayName("un étudiant ne peut pas lire le dossier d'un autre étudiant")
    void etudiantNeVoitPasLeDossierDunAutre() throws Exception {
        String moiId = creerEtudiant("ET-002", "Toure", "Awa", "BIO-1002");
        String autreId = creerEtudiant("ET-003", "Samake", "Bakary", "BIO-1003");

        MvcResult creation = mockMvc.perform(avecJeton(post("/etudiants/" + moiId + "/compte"), jetonAdmin))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode compte = donnees(creation);
        String jetonEtudiant = seConnecter(
                compte.get("email").asText(), compte.get("motDePasseInitial").asText());

        // Avant correction, ces deux endpoints étaient ouverts à tout compte authentifié.
        mockMvc.perform(avecJeton(get("/statistiques/etudiants/" + autreId), jetonEtudiant))
                .andExpect(status().isForbidden());
        mockMvc.perform(avecJeton(get("/justifications/etudiant/" + autreId), jetonEtudiant))
                .andExpect(status().isForbidden());

        // Ni, plus largement, le référentiel des étudiants.
        mockMvc.perform(avecJeton(get("/etudiants"), jetonEtudiant))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un étudiant accède à ses propres données sans jamais fournir d'identifiant")
    void etudiantAccedeASesPropresDonnees() throws Exception {
        String etudiantId = creerEtudiant("ET-004", "Diarra", "Fatoumata", "BIO-1004");
        MvcResult creation = mockMvc.perform(avecJeton(post("/etudiants/" + etudiantId + "/compte"), jetonAdmin))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode compte = donnees(creation);
        String jeton = seConnecter(compte.get("email").asText(), compte.get("motDePasseInitial").asText());

        mockMvc.perform(avecJeton(get("/moi/presences"), jeton)).andExpect(status().isOk());
        mockMvc.perform(avecJeton(get("/moi/statistiques"), jeton)).andExpect(status().isOk());
        mockMvc.perform(avecJeton(get("/moi/justificatifs"), jeton)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("un compte non rattaché à une fiche étudiant est refusé sur les données étudiantes")
    void compteSansFicheEtudianteEstRefuse() throws Exception {
        // L'administrateur est authentifié mais n'a pas le rôle ETUDIANT.
        mockMvc.perform(avecJeton(get("/moi/presences"), jetonAdmin))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("le profil d'un administrateur se résout sans profil métier")
    void profilAdministrateurSansRattachement() throws Exception {
        MvcResult resultat = mockMvc.perform(avecJeton(get("/moi/profil"), jetonAdmin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode profil = donnees(resultat);
        assertThat(profil.get("typeProfil").asText()).isEqualTo("AUCUN");
        assertThat(profil.get("roles").toString()).contains("ADMIN");
    }

    @Test
    @DisplayName("un second accès pour le même étudiant est refusé")
    void deuxiemeAccesEstRefuse() throws Exception {
        String etudiantId = creerEtudiant("ET-005", "Keita", "Salif", "BIO-1005");
        mockMvc.perform(avecJeton(post("/etudiants/" + etudiantId + "/compte"), jetonAdmin))
                .andExpect(status().isCreated());
        mockMvc.perform(avecJeton(post("/etudiants/" + etudiantId + "/compte"), jetonAdmin))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("la fermeture d'un accès conserve la fiche étudiant")
    void fermetureConserveLaFiche() throws Exception {
        String etudiantId = creerEtudiant("ET-006", "Dembele", "Aminata", "BIO-1006");
        mockMvc.perform(avecJeton(post("/etudiants/" + etudiantId + "/compte"), jetonAdmin))
                .andExpect(status().isCreated());

        MvcResult fermeture = mockMvc.perform(
                        avecJeton(delete("/etudiants/" + etudiantId + "/compte"), jetonAdmin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode etudiant = donnees(fermeture);
        assertThat(etudiant.get("compteOuvert").asBoolean()).isFalse();
        assertThat(etudiant.get("matricule").asText())
                .as("la fiche de scolarité survit à la fermeture du compte de consultation")
                .isEqualTo("ET-006");
    }

    // ------------------------------------------------------------------

    /** Crée une classe puis un étudiant qui lui est rattaché, et renvoie son identifiant. */
    private String creerEtudiant(String matricule, String nom, String prenom, String biometricId)
            throws Exception {
        long classeId = classeDeTest();
        MvcResult resultat = mockMvc.perform(avecJeton(post("/etudiants"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matricule":"%s","nom":"%s","prenom":"%s","biometricId":"%s",
                                 "classeId":%d,"actif":true}
                                """.formatted(matricule, nom, prenom, biometricId, classeId)))
                .andExpect(status().isCreated())
                .andReturn();
        return donnees(resultat).get("id").asText();
    }

    /**
     * Classe unique réutilisée par les tests de cette classe.
     *
     * <p>Une classe exige une promotion de rattachement : la hiérarchie
     * promotion → classe → étudiant est créée en entier au premier appel.</p>
     */
    private long classeDeTest() throws Exception {
        MvcResult liste = mockMvc.perform(avecJeton(get("/classes"), jetonAdmin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode classes = donnees(liste);
        if (classes.isArray() && !classes.isEmpty()) {
            return classes.get(0).get("id").asLong();
        }

        MvcResult promotion = mockMvc.perform(avecJeton(post("/promotions"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"PROMO-2026","libelle":"Promotion 2026","anneeAcademique":2026}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long promotionId = donnees(promotion).get("id").asLong();

        MvcResult creation = mockMvc.perform(avecJeton(post("/classes"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"L1-INFO","libelle":"Licence 1 Informatique","promotionId":%d}
                                """.formatted(promotionId)))
                .andExpect(status().isCreated())
                .andReturn();
        return donnees(creation).get("id").asLong();
    }
}
