package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Intégration — enrôlement biométrique en libre-service.
 *
 * <p>L'enjeu tient en une phrase : deux machines qui ne se parlent pas — le téléphone
 * de l'étudiant et le lecteur — doivent aboutir à une seule association, la bonne.
 * Ce que ces tests vérifient, et qu'aucune relecture ne garantit, c'est que le
 * rendez-vous se referme sur le bon dossier, qu'il ne se rejoue pas, et que le
 * lecteur n'apprend au passage aucun identifiant interne.</p>
 */
@DisplayName("Intégration — enrôlement biométrique en libre-service")
class EnrolementLibreServiceTest extends IntegrationTestBase {

    /** Rend chaque décor distinct : les tests d'une même classe partagent la base. */
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    @Test
    @DisplayName("l'étudiant demande, le lecteur appelle, l'empreinte est portée à son dossier")
    void parcoursComplet() throws Exception {
        Etudiant etudiant = inscrire("A");
        Lecteur lecteur = declarerLecteur("A");

        // 1. Depuis son téléphone : « je veux enrôler mon empreinte ».
        JsonNode demande = donnees(mockMvc.perform(
                        avecJeton(post("/moi/enrolement"), etudiant.jeton))
                .andExpect(status().isCreated())
                .andReturn());

        String code = demande.get("code").asText();
        assertThat(code).as("un code à six chiffres, lisible sur un écran de 128×64")
                .matches("\\d{6}");
        assertThat(demande.get("statut").asText()).isEqualTo("EN_ATTENTE");
        assertThat(demande.get("enrole").asBoolean()).isFalse();

        // 2. Devant le capteur : le lecteur demande qui est attendu.
        JsonNode appel = servirJusquAu(lecteur, code);
        assertThat(appel.get("nomAffiche").asText())
                .as("de quoi appeler l'intéressé à voix haute")
                .contains(etudiant.nom);
        assertThat(appel.get("matricule").asText()).isEqualTo(etudiant.matricule);
        assertThat(appel.get("secondesRestantes").asLong()).isPositive();

        // 3. Doigt posé : le lecteur annonce l'emplacement occupé dans sa mémoire.
        mockMvc.perform(post("/esp32/enrolements")
                        .header("X-API-KEY", lecteur.cleApi)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","reference":"%s"}
                                """.formatted(code, lecteur.reference)))
                .andExpect(status().isOk());

        // 4. L'application bascule : l'étudiant est reconnu.
        JsonNode apres = donnees(mockMvc.perform(
                        avecJeton(get("/moi/enrolement"), etudiant.jeton))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(apres.get("statut").asText()).isEqualTo("TERMINEE");
        assertThat(apres.get("enrole").asBoolean()).isTrue();

        // 5. Et le dossier tenu par la scolarité dit la même chose.
        JsonNode fiche = donnees(mockMvc.perform(
                        avecJeton(get("/etudiants/" + etudiant.id), jetonAdmin))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(fiche.get("enrole").asBoolean()).isTrue();
        assertThat(fiche.get("biometricId").asText()).isEqualTo(lecteur.reference);
    }

    @Test
    @DisplayName("le lecteur n'apprend aucun identifiant interne")
    void leLecteurNeVoitNiUuidNiGabarit() throws Exception {
        Etudiant etudiant = inscrire("B");
        Lecteur lecteur = declarerLecteur("B");

        String code = donnees(mockMvc.perform(avecJeton(post("/moi/enrolement"), etudiant.jeton))
                .andExpect(status().isCreated())
                .andReturn()).get("code").asText();

        MvcResult appel = servirJusquAuBrut(lecteur, code);
        String charge = appel.getResponse().getContentAsString();

        // La règle du projet : le lecteur ne connaît que des références logiques.
        assertThat(charge)
                .as("l'UUID de l'étudiant ne doit jamais franchir la frontière du lecteur")
                .doesNotContain(etudiant.id);
        assertThat(charge)
                .as("ni celui de son compte de connexion")
                .doesNotContain(etudiant.utilisateurId);
    }

    @Test
    @DisplayName("rouvrir l'écran ne produit pas un second code")
    void demandeIdempotente() throws Exception {
        Etudiant etudiant = inscrire("C");

        String premier = donnees(mockMvc.perform(avecJeton(post("/moi/enrolement"), etudiant.jeton))
                .andExpect(status().isCreated())
                .andReturn()).get("code").asText();

        String second = donnees(mockMvc.perform(avecJeton(post("/moi/enrolement"), etudiant.jeton))
                .andExpect(status().isCreated())
                .andReturn()).get("code").asText();

        assertThat(second)
                .as("le premier code est peut-être déjà sous les yeux de l'étudiant")
                .isEqualTo(premier);

        // Renoncer referme le rendez-vous, et le code cesse d'être servi.
        mockMvc.perform(avecJeton(delete("/moi/enrolement"), etudiant.jeton))
                .andExpect(status().isOk());
        mockMvc.perform(avecJeton(delete("/moi/enrolement"), etudiant.jeton))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("une capture sans demande ouverte n'est portée à personne")
    void captureSansDemandeRefusee() throws Exception {
        Lecteur lecteur = declarerLecteur("D");

        mockMvc.perform(post("/esp32/enrolements")
                        .header("X-API-KEY", lecteur.cleApi)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"000000","reference":"%s"}
                                """.formatted(lecteur.reference)))
                .andExpect(status().isNotFound());

        // Sans clé d'API valable, le lecteur n'est même pas un lecteur.
        mockMvc.perform(get("/esp32/enrolements/prochain")
                        .header("X-API-KEY", "CLE-QUI-N-EXISTE-PAS"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un étudiant déjà enrôlé ne peut pas se réenrôler seul")
    void reenrolementRefuse() throws Exception {
        Etudiant etudiant = inscrire("E");
        Lecteur lecteur = declarerLecteur("E");

        String code = donnees(mockMvc.perform(avecJeton(post("/moi/enrolement"), etudiant.jeton))
                .andExpect(status().isCreated())
                .andReturn()).get("code").asText();
        servirJusquAu(lecteur, code);
        mockMvc.perform(post("/esp32/enrolements")
                        .header("X-API-KEY", lecteur.cleApi)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","reference":"%s"}
                                """.formatted(code, lecteur.reference)))
                .andExpect(status().isOk());

        // Chaque tentative laisserait derrière elle un emplacement occupé pour rien
        // dans la mémoire du capteur : la révocation reste un geste de la scolarité.
        mockMvc.perform(avecJeton(post("/moi/enrolement"), etudiant.jeton))
                .andExpect(status().isConflict());

        // Rejouer la capture ne doit pas davantage aboutir.
        mockMvc.perform(post("/esp32/enrolements")
                        .header("X-API-KEY", lecteur.cleApi)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","reference":"%s"}
                                """.formatted(code, lecteur.reference)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("seul un étudiant ouvre une demande pour lui-même")
    void demandeReserveeAuxEtudiants() throws Exception {
        mockMvc.perform(avecJeton(post("/moi/enrolement"), jetonAdmin))
                .andExpect(status().isForbidden());
        // 403 et non 401 : sans jeton, la requête parvient au contrôleur sans identité,
        // et c'est l'autorisation qui la refuse — même convention que pour les lecteurs.
        mockMvc.perform(post("/moi/enrolement"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Décor
    // ------------------------------------------------------------------

    private record Etudiant(String id, String utilisateurId, String matricule,
                            String nom, String jeton) {}

    private record Lecteur(String id, String cleApi, String reference) {}

    /**
     * Inscrit un étudiant par la voie publique et ouvre sa session.
     *
     * <p>Passer par l'inscription libre plutôt que par la création administrative :
     * c'est le parcours réel d'un étudiant qui va enrôler son empreinte.</p>
     */
    private Etudiant inscrire(String cle) throws Exception {
        String suffixe = cle + COMPTEUR.incrementAndGet();
        long classeId = classeDeTest();
        String numero = "CEN-ENR-" + suffixe;

        mockMvc.perform(avecJeton(post("/cenou"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numero":"%s"}
                                """.formatted(numero)))
                .andExpect(status().isCreated());

        JsonNode etudiant = donnees(mockMvc.perform(post("/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numeroCenou":"%s","nom":"TRAORE","prenom":"Fatoumata",
                                 "classeId":%d,"motDePasse":"MotDePasse2026"}
                                """.formatted(numero, classeId)))
                .andExpect(status().isCreated())
                .andReturn());

        String jeton = seConnecter(
                numero.toLowerCase() + "@etudiant.smartpresence.local", "MotDePasse2026");

        return new Etudiant(etudiant.get("id").asText(),
                etudiant.get("utilisateurId").asText(),
                etudiant.get("matricule").asText(),
                "TRAORE", jeton);
    }

    private Lecteur declarerLecteur(String cle) throws Exception {
        int rang = COMPTEUR.incrementAndGet();
        String mac = "DE:AD:BE:EF:%02X:%02X".formatted(rang / 256, rang % 256);
        String cleApi = "CLE-ENROLEMENT-" + cle + rang;

        JsonNode device = donnees(mockMvc.perform(avecJeton(post("/devices"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Lecteur enrolement %s","adresseMac":"%s","apiKey":"%s",
                                 "statut":"ACTIF","usage":"MIXTE"}
                                """.formatted(cle + rang, mac, cleApi)))
                .andExpect(status().isCreated())
                .andReturn());

        return new Lecteur(device.get("id").asText(), cleApi, "ETU-%04d".formatted(rang));
    }

    /**
     * Interroge le lecteur jusqu'à ce qu'il appelle le code attendu.
     *
     * <p>La file est commune à toute la classe de tests : une demande laissée ouverte
     * par un autre scénario passerait avant. Les servir d'abord rend ce test
     * indépendant de l'ordre d'exécution, sans rien relâcher de ce qu'il vérifie.</p>
     */
    private JsonNode servirJusquAu(Lecteur lecteur, String code) throws Exception {
        return donnees(servirJusquAuBrut(lecteur, code));
    }

    private MvcResult servirJusquAuBrut(Lecteur lecteur, String code) throws Exception {
        for (int appel = 0; appel < 20; appel++) {
            MvcResult resultat = mockMvc.perform(get("/esp32/enrolements/prochain")
                            .header("X-API-KEY", lecteur.cleApi))
                    .andReturn();

            assertThat(resultat.getResponse().getStatus())
                    .as("le code %s n'a jamais été appelé : la file s'est vidée avant", code)
                    .isEqualTo(200);

            if (donnees(resultat).get("code").asText().equals(code)) {
                return resultat;
            }
        }
        throw new AssertionError("Le code " + code + " n'a pas été appelé en 20 tentatives");
    }

    /** Classe de rattachement, créée avec sa promotion au premier appel. */
    private long classeDeTest() throws Exception {
        JsonNode classes = donnees(mockMvc.perform(avecJeton(get("/classes"), jetonAdmin))
                .andExpect(status().isOk())
                .andReturn());
        if (classes.isArray() && !classes.isEmpty()) {
            return classes.get(0).get("id").asLong();
        }

        long promotionId = donnees(mockMvc.perform(avecJeton(post("/promotions"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"PROMO-ENR","libelle":"Promotion d'essai","anneeAcademique":2026}
                                """))
                .andExpect(status().isCreated())
                .andReturn()).get("id").asLong();

        return donnees(mockMvc.perform(avecJeton(post("/classes"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"CL-ENR","libelle":"Classe d'essai","promotionId":%d}
                                """.formatted(promotionId)))
                .andExpect(status().isCreated())
                .andReturn()).get("id").asLong();
    }
}
