package com.smartpresence.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Intégration — changement de mot de passe")
class MotDePasseTest extends IntegrationTestBase {

    @Test
    @DisplayName("l'ancien mot de passe est exigé, puis seul le nouveau ouvre la session")
    void changement() throws Exception {
        String email = "mdp-test@smartpresence.local";
        mockMvc.perform(avecJeton(post("/auth/register"), jetonAdmin).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","motDePasse":"AncienMotDePasse1","nom":"Test","prenom":"Mdp","roles":["RH"]}
                        """.formatted(email))).andExpect(status().isCreated());
        String jeton = seConnecter(email, "AncienMotDePasse1");

        // Mauvais mot de passe actuel, nouveau trop court, nouveau identique : trois refus.
        modifier(jeton, "Mauvais", "NouveauMotDePasse2", 400);
        modifier(jeton, "AncienMotDePasse1", "court", 400);
        modifier(jeton, "AncienMotDePasse1", "AncienMotDePasse1", 400);

        modifier(jeton, "AncienMotDePasse1", "NouveauMotDePasse2", 200);

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"motDePasse\":\"AncienMotDePasse1\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
        seConnecter(email, "NouveauMotDePasse2");
    }

    @Test
    @DisplayName("sans jeton, le changement est refusé")
    void sansJeton() throws Exception {
        mockMvc.perform(patch("/auth/mot-de-passe").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motDePasseActuel\":\"a\",\"nouveauMotDePasse\":\"NouveauMotDePasse2\"}"))
                .andExpect(status().is4xxClientError());
    }

    private void modifier(String jeton, String actuel, String nouveau, int attendu) throws Exception {
        mockMvc.perform(avecJeton(patch("/auth/mot-de-passe"), jeton).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motDePasseActuel\":\"%s\",\"nouveauMotDePasse\":\"%s\"}".formatted(actuel, nouveau)))
                .andExpect(status().is(attendu));
    }
}
