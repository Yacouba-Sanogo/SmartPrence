package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Socle commun des tests d'intégration.
 *
 * <p>Les requêtes traversent la <b>chaîne complète</b> : filtres Spring Security,
 * contrôleurs, services, Hibernate et MySQL. C'est ce qui distingue ces tests des
 * tests unitaires à mocks — seule cette traversée intégrale met en évidence les
 * défauts de mapping ou de configuration de sécurité.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${app.security.bootstrap.email}")
    private String emailAdmin;

    @Value("${app.security.bootstrap.password}")
    private String motDePasseAdmin;

    /** Jeton de l'administrateur d'amorçage, obtenu par une vraie connexion. */
    protected String jetonAdmin;

    @BeforeEach
    void authentifierAdministrateur() throws Exception {
        jetonAdmin = seConnecter(emailAdmin, motDePasseAdmin);
    }

    /**
     * Ouvre une session et retourne le jeton d'accès.
     *
     * @throws AssertionError si la connexion échoue — un test ne doit jamais
     *                        poursuivre avec un jeton absent
     */
    protected String seConnecter(String email, String motDePasse) throws Exception {
        String corps = """
                {"email": "%s", "motDePasse": "%s"}
                """.formatted(email, motDePasse);

        MvcResult resultat = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isOk())
                .andReturn();

        return donnees(resultat).get("accessToken").asText();
    }

    /** Ajoute l'en-tête d'autorisation JWT à une requête. */
    protected MockHttpServletRequestBuilder avecJeton(MockHttpServletRequestBuilder requete, String jeton) {
        return requete.header("Authorization", "Bearer " + jeton);
    }

    /** Extrait le nœud {@code data} de l'enveloppe de réponse uniforme. */
    protected JsonNode donnees(MvcResult resultat) throws Exception {
        return objectMapper
                .readTree(resultat.getResponse().getContentAsString())
                .get("data");
    }

    protected String enJson(Object valeur) throws Exception {
        return objectMapper.writeValueAsString(valeur);
    }
}
