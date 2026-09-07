package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration de l'authentification et du contrôle d'accès.
 *
 * <p>Chacun couvre un défaut réellement rencontré sur le projet et qu'aucun test
 * unitaire à mocks n'aurait pu détecter.</p>
 */
@DisplayName("Intégration — authentification et autorisations")
class AuthentificationIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("la connexion renvoie les rôles de l'utilisateur, dans la réponse comme dans le jeton")
    void connexionRenvoieLesRoles() throws Exception {
        MvcResult resultat = mockMvc.perform(avecJeton(get("/users/me"), jetonAdmin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode profil = donnees(resultat);
        assertThat(profil.get("email").asText()).isEqualTo("admin.test@smartpresence.local");

        // Le défaut d'origine : les rôles remontaient vides parce que la jointure
        // sur l'identifiant UUID ne trouvait jamais rien. L'utilisateur était
        // authentifié mais dépourvu de toute autorisation.
        JsonNode roles = profil.get("roles");
        assertThat(roles).isNotNull();
        assertThat(roles.size())
                .as("l'administrateur d'amorçage doit porter au moins un rôle")
                .isPositive();
    }

    @Test
    @DisplayName("le jeton JWT transporte les autorités de l'utilisateur")
    void jetonContientLesAutorites() throws Exception {
        String charge = jetonAdmin.split("\\.")[1];
        JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(charge));

        assertThat(claims.get("roles")).isNotNull();
        assertThat(claims.get("roles").toString())
                .as("un jeton sans rôle rend tous les endpoints protégés inaccessibles")
                .contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("un endpoint protégé est accessible avec un jeton valide")
    void endpointProtegeAccessibleAvecJeton() throws Exception {
        mockMvc.perform(avecJeton(get("/personnels"), jetonAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("un endpoint protégé est refusé sans jeton")
    void endpointProtegeRefuseSansJeton() throws Exception {
        mockMvc.perform(get("/personnels"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("la création de compte est fermée aux anonymes")
    void inscriptionLibreEstFermee() throws Exception {
        // Avant correction, /auth/** était entièrement public : n'importe qui
        // pouvait se créer un compte ADMIN sans s'authentifier.
        String corps = """
                {"email":"intrus@exemple.com","motDePasse":"motdepasse","nom":"Intrus",
                 "prenom":"Anonyme","roles":["ADMIN"]}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un administrateur authentifié peut créer un compte")
    void administrateurPeutCreerUnCompte() throws Exception {
        String corps = """
                {"email":"rh@smartpresence.local","motDePasse":"MotDePasseRh2026","nom":"Diarra",
                 "prenom":"Fatoumata","roles":["RH"]}
                """;

        mockMvc.perform(avecJeton(post("/auth/register"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated());

        // Le compte créé doit pouvoir se connecter et porter son rôle : c'est la
        // preuve que la jointure utilisateur ↔ rôle fonctionne à l'écriture aussi.
        String jetonRh = seConnecter("rh@smartpresence.local", "MotDePasseRh2026");
        String charge = jetonRh.split("\\.")[1];
        JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(charge));
        assertThat(claims.get("roles").toString()).contains("ROLE_RH");
    }

    @Test
    @DisplayName("un rôle insuffisant est refusé sur un endpoint réservé")
    void roleInsuffisantEstRefuse() throws Exception {
        String corps = """
                {"email":"enseignant@smartpresence.local","motDePasse":"MotDePasseEns2026",
                 "nom":"Keita","prenom":"Ibrahim","roles":["ENSEIGNANT"]}
                """;

        mockMvc.perform(avecJeton(post("/auth/register"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated());

        String jetonEnseignant = seConnecter("enseignant@smartpresence.local", "MotDePasseEns2026");

        // /personnels est réservé à ADMIN, RH, RESPONSABLE_SCOLARITE et SUPERVISEUR.
        mockMvc.perform(avecJeton(get("/personnels"), jetonEnseignant))
                .andExpect(status().isForbidden());
    }
}
