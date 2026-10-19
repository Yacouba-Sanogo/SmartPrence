package com.smartpresence.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Matrice des permissions, dérivée du code lui-même.
 *
 * <p>Pour chaque route {@code GET} sans paramètre de chemin, le test lit l'annotation
 * {@code @PreAuthorize} et interroge la route avec chaque profil. Un profil non autorisé
 * doit recevoir la réponse de refus du filtre de sécurité ; un profil autorisé ne doit
 * jamais la recevoir. Cela garde deux choses : qu'aucune route n'ait été oubliée, et que
 * la sécurité par méthode soit réellement active — une annotation sans effet ne se voit
 * pas à la lecture.</p>
 */
@DisplayName("Intégration — matrice des rôles et permissions")
class MatricePermissionsTest extends IntegrationTestBase {

    private static final String REFUS = "Accès refusé. Privilèges insuffisants.";
    private static final List<String> PROFILS =
            List.of("ADMIN", "RESPONSABLE_SCOLARITE", "SUPERVISEUR", "RH", "ENSEIGNANT", "ETUDIANT");

    /** Routes volontairement publiques : tout le reste doit porter une règle. */
    private static final Set<String> PUBLIQUES = Set.of(
            "/ping", "/concours/regles", "/inscription/classes",
            "/referentiel/departements", "/referentiel/filieres");

    @Autowired
    private RequestMappingHandlerMapping mappings;

    @Test
    @DisplayName("chaque profil n'atteint que les routes que son rôle autorise")
    void matriceDesRoutes() throws Exception {
        Map<String, String> jetons = new LinkedHashMap<>();
        jetons.put("ADMIN", jetonAdmin);
        for (String role : PROFILS.subList(1, PROFILS.size())) {
            String email = "matrice-" + role.toLowerCase() + "@smartpresence.local";
            mockMvc.perform(avecJeton(post("/auth/register"), jetonAdmin).contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"email":"%s","motDePasse":"MotDePasseMatrice2026","nom":"Matrice","prenom":"%s","roles":["%s"]}
                            """.formatted(email, role, role)));
            jetons.put(role, seConnecter(email, "MotDePasseMatrice2026"));
        }

        int verifiees = 0;
        int indetermines = 0;
        List<String> anomalies = new ArrayList<>();
        List<String> sansRegle = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> e : mappings.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = e.getKey();
            if (!info.getMethodsCondition().getMethods().stream().anyMatch(m -> m.name().equals("GET"))) continue;
            Set<String> chemins = info.getPathPatternsCondition().getPatternValues();
            String chemin = chemins.iterator().next();
            // Le contexte (/api) n'entre pas dans MockMvc ; les routes à variable et les
            // outils d'exploitation sont hors périmètre.
            if (chemin.contains("{") || chemin.startsWith("/error") || chemin.startsWith("/v3")
                    || chemin.startsWith("/swagger") || chemin.startsWith("/esp32")) continue;

            PreAuthorize regle = e.getValue().getMethodAnnotation(PreAuthorize.class);
            if (regle == null) {
                regle = e.getValue().getBeanType().getAnnotation(PreAuthorize.class);
            }
            if (regle == null) {
                if (!PUBLIQUES.contains(chemin)) sansRegle.add(chemin);
                continue;
            }
            Set<String> autorises = rolesAutorises(regle.value());
            if (autorises.contains("DEVICE") && autorises.size() == 1) continue;

            for (String profil : PROFILS) {
                MvcResult r = mockMvc.perform(avecJeton(get(chemin), jetons.get(profil))).andReturn();
                String corps = r.getResponse().getContentAsString();
                boolean refuse = corps.contains(REFUS);
                boolean attendu = autorises.contains("*") || autorises.contains(profil);
                // Une route à paramètre obligatoire répond 400 avant que la règle ne soit
                // évaluée : le refus ne peut alors ni se prouver ni se réfuter.
                if (!attendu && !refuse && r.getResponse().getStatus() == 400) {
                    indetermines++;
                    continue;
                }
                if (attendu == refuse || r.getResponse().getStatus() == 401) {
                    anomalies.add("%s avec %s : attendu %s, reçu %d".formatted(
                            chemin, profil, attendu ? "accès" : "refus", r.getResponse().getStatus()));
                }
                verifiees++;
            }
            // Sans jeton, aucune de ces routes ne répond.
            int sansJeton = mockMvc.perform(get(chemin)).andReturn().getResponse().getStatus();
            if (sansJeton != 401 && sansJeton != 403) {
                anomalies.add("%s sans jeton : reçu %d".formatted(chemin, sansJeton));
            }
        }

        assertThat(sansRegle).as("routes GET sans règle d'accès, hors liste publique").isEmpty();
        assertThat(anomalies).as("écarts entre la règle annoncée et le comportement réel").isEmpty();
        assertThat(indetermines).as("routes dont le refus n'a pas pu être éprouvé (paramètre requis)")
                .isLessThan(verifiees / 10);
        assertThat(verifiees).as("la matrice doit réellement couvrir des routes").isGreaterThan(60);
    }

    /** Rôles cités dans l'expression ; « * » pour {@code isAuthenticated()}. */
    private static Set<String> rolesAutorises(String expression) {
        if (expression.contains("isAuthenticated()")) return Set.of("*");
        Matcher m = Pattern.compile("'([A-Z_]+)'").matcher(expression);
        Set<String> roles = new java.util.HashSet<>();
        while (m.find()) roles.add(m.group(1));
        return roles;
    }
}
