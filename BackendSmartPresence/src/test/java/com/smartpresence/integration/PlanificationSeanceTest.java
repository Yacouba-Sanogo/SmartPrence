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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Planification des séances.
 *
 * <p>L'emploi du temps acceptait jusqu'ici n'importe quel créneau : une classe pouvait
 * suivre deux cours simultanés, un enseignant en assurer deux, une salle en accueillir
 * deux. Ces situations sont impossibles dans le monde réel, et elles rendraient les
 * relevés du lecteur inattribuables.</p>
 */
@DisplayName("Intégration — planification des séances")
class PlanificationSeanceTest extends IntegrationTestBase {

    @Test
    @DisplayName("une classe ne peut pas suivre deux cours au même moment")
    void classeIndisponible() throws Exception {
        Decor d = preparer("PLA-A");
        creerSeance(d, d.enseignantA, d.salleA, 8, 10).andExpect(status().isCreated());

        // Autre enseignant, autre salle, mais la même classe : impossible.
        creerSeance(d, d.enseignantB, d.salleB, 9, 11)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("un enseignant ne peut pas assurer deux cours au même moment")
    void enseignantIndisponible() throws Exception {
        Decor d = preparer("PLA-B");
        creerSeance(d, d.enseignantA, d.salleA, 8, 10).andExpect(status().isCreated());

        mockMvc.perform(avecJeton(post("/seances"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsSeance(d.classeB, d.matiereId, d.enseignantA,
                                d.salleB, 9, 11)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("une salle ne peut pas accueillir deux cours au même moment")
    void salleIndisponible() throws Exception {
        Decor d = preparer("PLA-C");
        creerSeance(d, d.enseignantA, d.salleA, 8, 10).andExpect(status().isCreated());

        mockMvc.perform(avecJeton(post("/seances"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsSeance(d.classeB, d.matiereId, d.enseignantB,
                                d.salleA, 9, 11)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("une séance entièrement contenue dans une autre est bien détectée")
    void chevauchementParInclusion() throws Exception {
        Decor d = preparer("PLA-D");
        creerSeance(d, d.enseignantA, d.salleA, 8, 12).andExpect(status().isCreated());

        // Comparer seulement les heures de début laisserait passer ce cas.
        creerSeance(d, d.enseignantB, d.salleB, 9, 10)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("deux séances qui se suivent sans se recouvrir sont acceptées")
    void creneauxAdjacentsAcceptes() throws Exception {
        Decor d = preparer("PLA-E");
        creerSeance(d, d.enseignantA, d.salleA, 8, 10).andExpect(status().isCreated());

        // 10:00–12:00 commence exactement quand la précédente finit : pas de conflit.
        creerSeance(d, d.enseignantA, d.salleA, 10, 12).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("une séance annulée libère son créneau")
    void seanceAnnuleeLibereLeCreneau() throws Exception {
        Decor d = preparer("PLA-F");
        String seanceId = donnees(creerSeance(d, d.enseignantA, d.salleA, 8, 10)
                .andExpect(status().isCreated()).andReturn()).get("id").asText();

        mockMvc.perform(avecJeton(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/seances/" + seanceId + "/statut")
                                .param("value", "ANNULEE"), jetonAdmin))
                .andExpect(status().isOk());

        creerSeance(d, d.enseignantA, d.salleA, 8, 10).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("modifier une séance ne la détecte pas comme son propre conflit")
    void modificationSansAutoConflit() throws Exception {
        Decor d = preparer("PLA-G");
        String seanceId = donnees(creerSeance(d, d.enseignantA, d.salleA, 8, 10)
                .andExpect(status().isCreated()).andReturn()).get("id").asText();

        // Même créneau, seule la note change : sans exclusion, la séance se verrait
        // elle-même et la modification serait impossible.
        mockMvc.perform(avecJeton(put("/seances/" + seanceId), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsSeance(d.classeA, d.matiereId, d.enseignantA,
                                d.salleA, 8, 10)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("une séance dont les présences sont relevées ne peut plus être modifiée")
    void seanceReleveeEstFigee() throws Exception {
        Decor d = preparer("PLA-H");
        String seanceId = donnees(creerSeance(d, d.enseignantA, d.salleA, 8, 10)
                .andExpect(status().isCreated()).andReturn()).get("id").asText();

        mockMvc.perform(avecJeton(post("/presences/manual"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etudiantId":"%s","seanceId":"%s","datePresence":"%s",
                                 "heurePresence":"08:05:00","statut":"PRESENT"}
                                """.formatted(d.etudiantId, seanceId, LocalDate.now())))
                .andExpect(status().isCreated());

        // Déplacer le créneau requalifierait rétroactivement ce relevé.
        mockMvc.perform(avecJeton(put("/seances/" + seanceId), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsSeance(d.classeA, d.matiereId, d.enseignantA,
                                d.salleA, 14, 16)))
                .andExpect(status().isConflict());

        mockMvc.perform(avecJeton(delete("/seances/" + seanceId), jetonAdmin))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("une séance sans relevé se supprime")
    void seanceVierge() throws Exception {
        Decor d = preparer("PLA-I");
        String seanceId = donnees(creerSeance(d, d.enseignantA, d.salleA, 8, 10)
                .andExpect(status().isCreated()).andReturn()).get("id").asText();

        mockMvc.perform(avecJeton(delete("/seances/" + seanceId), jetonAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("une matière utilisée par une séance ne se supprime pas")
    void matiereUtilisee() throws Exception {
        Decor d = preparer("PLA-J");
        creerSeance(d, d.enseignantA, d.salleA, 8, 10).andExpect(status().isCreated());

        mockMvc.perform(avecJeton(delete("/matieres/" + d.matiereId), jetonAdmin))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("l'emploi du temps se filtre par classe et par enseignant")
    void filtresDeLEmploiDuTemps() throws Exception {
        Decor d = preparer("PLA-K");
        creerSeance(d, d.enseignantA, d.salleA, 8, 10).andExpect(status().isCreated());
        mockMvc.perform(avecJeton(post("/seances"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsSeance(d.classeB, d.matiereId, d.enseignantB,
                                d.salleB, 8, 10)))
                .andExpect(status().isCreated());

        String jour = LocalDate.now().toString();

        // Les tests de cette classe partagent la base : compter le total renseignerait
        // sur eux plutôt que sur le filtre. Seul le contenu filtré est vérifiable.
        JsonNode parClasse = donnees(mockMvc.perform(avecJeton(get("/seances")
                        .param("debut", jour).param("fin", jour)
                        .param("classeId", String.valueOf(d.classeA)), jetonAdmin))
                .andExpect(status().isOk()).andReturn());
        assertThat(parClasse).hasSize(1);
        assertThat(parClasse.get(0).get("classeId").asLong()).isEqualTo(d.classeA);

        JsonNode parEnseignant = donnees(mockMvc.perform(avecJeton(get("/seances")
                        .param("debut", jour).param("fin", jour)
                        .param("enseignantId", d.enseignantB), jetonAdmin))
                .andExpect(status().isOk()).andReturn());
        assertThat(parEnseignant).hasSize(1);
        assertThat(parEnseignant.get(0).get("enseignantId").asText()).isEqualTo(d.enseignantB);

        // Hors période, plus rien : la borne de fin est bien exclusive au jour suivant.
        assertThat(donnees(mockMvc.perform(avecJeton(get("/seances")
                        .param("debut", LocalDate.now().plusDays(7).toString())
                        .param("fin", LocalDate.now().plusDays(7).toString())
                        .param("classeId", String.valueOf(d.classeA)), jetonAdmin))
                .andExpect(status().isOk()).andReturn())).isEmpty();
    }

    @Test
    @DisplayName("une séance ne peut pas finir avant de commencer")
    void creneauInverse() throws Exception {
        Decor d = preparer("PLA-L");
        mockMvc.perform(avecJeton(post("/seances"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsSeance(d.classeA, d.matiereId, d.enseignantA,
                                d.salleA, 12, 8)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------

    private record Decor(long classeA, long classeB, long matiereId, long salleA, long salleB,
                         String enseignantA, String enseignantB, String etudiantId) {}

    private org.springframework.test.web.servlet.ResultActions creerSeance(
            Decor d, String enseignant, long salle, int heureDebut, int heureFin)
            throws Exception {
        return mockMvc.perform(avecJeton(post("/seances"), jetonAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpsSeance(d.classeA, d.matiereId, enseignant, salle,
                        heureDebut, heureFin)));
    }

    private String corpsSeance(long classeId, long matiereId, String enseignantId,
                               long salleId, int heureDebut, int heureFin) {
        ZoneId zone = ZoneId.systemDefault();
        Instant debut = LocalDate.now().atTime(heureDebut, 0).atZone(zone).toInstant();
        Instant fin = LocalDate.now().atTime(heureFin, 0).atZone(zone).toInstant();
        return """
                {"classeId":%d,"matiereId":%d,"enseignantId":"%s","salleId":%d,
                 "debut":"%s","fin":"%s"}
                """.formatted(classeId, matiereId, enseignantId, salleId, debut, fin);
    }

    private Decor preparer(String cle) throws Exception {
        long promotionId = creer("/promotions", """
                {"code":"PROMO-%s","libelle":"Promotion %s","anneeAcademique":2026}
                """.formatted(cle, cle)).get("id").asLong();

        long classeA = creer("/classes", """
                {"code":"%s-A","libelle":"Classe A %s","promotionId":%d}
                """.formatted(cle, cle, promotionId)).get("id").asLong();
        long classeB = creer("/classes", """
                {"code":"%s-B","libelle":"Classe B %s","promotionId":%d}
                """.formatted(cle, cle, promotionId)).get("id").asLong();

        String enseignantA = creer("/personnels", """
                {"matricule":"PROF-%s-A","nom":"Kone","prenom":"Ali","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();
        String enseignantB = creer("/personnels", """
                {"matricule":"PROF-%s-B","nom":"Sow","prenom":"Bina","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();

        long matiereId = creer("/matieres", """
                {"code":"MAT-%s","libelle":"Matiere %s","credits":3}
                """.formatted(cle, cle)).get("id").asLong();

        long salleA = creer("/salles", """
                {"code":"%s-S1","nom":"Salle 1 %s","capacite":40}
                """.formatted(cle, cle)).get("id").asLong();
        long salleB = creer("/salles", """
                {"code":"%s-S2","nom":"Salle 2 %s","capacite":40}
                """.formatted(cle, cle)).get("id").asLong();

        String etudiantId = creer("/etudiants", """
                {"matricule":"%s-ET1","nom":"Diarra","prenom":"Ami","classeId":%d,"actif":true}
                """.formatted(cle, classeA)).get("id").asText();

        return new Decor(classeA, classeB, matiereId, salleA, salleB,
                enseignantA, enseignantB, etudiantId);
    }

    private JsonNode creer(String chemin, String corps) throws Exception {
        MvcResult resultat = mockMvc.perform(avecJeton(post(chemin), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andReturn();
        return donnees(resultat);
    }
}
