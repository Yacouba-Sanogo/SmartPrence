package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Intégration — emploi du temps de l'étudiant.
 *
 * <p>L'emploi du temps n'était visible que de l'administration et des enseignants.
 * L'étudiant — premier concerné par l'heure de son prochain cours — n'y avait aucun
 * accès. Ce qui se vérifie ici : qu'il voie les séances de sa classe, et celles-là
 * seulement.</p>
 */
@DisplayName("Intégration — emploi du temps de l'étudiant")
class EmploiDuTempsEtudiantTest extends IntegrationTestBase {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Test
    @DisplayName("l'étudiant voit les séances de sa classe, pas celles des autres")
    void seulesLesSeancesDeSaClasse() throws Exception {
        Decor d = preparer("EDT-A");

        planifier(d, d.classeId, LocalDate.now(), 8);
        planifier(d, d.autreClasseId, LocalDate.now(), 10);

        JsonNode seances = emploiDuTemps(d, null, null);
        assertThat(seances).hasSize(1);
        assertThat(seances.get(0).get("classeId").asLong()).isEqualTo(d.classeId);
    }

    @Test
    @DisplayName("sans paramètre, les sept jours à venir")
    void fenetreParDefautDUneSemaine() throws Exception {
        Decor d = preparer("EDT-B");

        planifier(d, d.classeId, LocalDate.now(), 8);
        planifier(d, d.classeId, LocalDate.now().plusDays(6), 8);

        // Au-delà de la fenêtre : la séance existe, mais l'écran ne s'en encombre pas.
        planifier(d, d.classeId, LocalDate.now().plusDays(20), 8);

        assertThat(emploiDuTemps(d, null, null)).hasSize(2);

        // Fenêtre explicite : la séance lointaine réapparaît.
        assertThat(emploiDuTemps(d, LocalDate.now(), LocalDate.now().plusDays(30))).hasSize(3);
    }

    @Test
    @DisplayName("les séances sont rendues dans l'ordre du temps")
    void ordreChronologique() throws Exception {
        Decor d = preparer("EDT-C");

        // Créées à l'envers : l'ordre ne doit rien à celui de la saisie.
        planifier(d, d.classeId, LocalDate.now().plusDays(2), 14);
        planifier(d, d.classeId, LocalDate.now(), 8);

        JsonNode seances = emploiDuTemps(d, null, null);
        assertThat(seances).hasSize(2);
        assertThat(seances.get(0).get("debut").asText())
                .isLessThan(seances.get(1).get("debut").asText());
    }

    @Test
    @DisplayName("une fenêtre à l'envers est refusée, et l'espace reste celui de l'étudiant")
    void gardeFous() throws Exception {
        Decor d = preparer("EDT-D");

        mockMvc.perform(avecJeton(get("/moi/emploi-du-temps"), d.jetonEtudiant)
                        .param("debut", LocalDate.now().plusDays(5).toString())
                        .param("fin", LocalDate.now().toString()))
                .andExpect(status().isBadRequest());

        // L'adresse ne prend aucun identifiant : un administrateur n'y a rien à faire.
        mockMvc.perform(avecJeton(get("/moi/emploi-du-temps"), jetonAdmin))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------

    private record Decor(long classeId, long autreClasseId, long matiereId, long salleId,
                         String enseignantId, String jetonEtudiant) {}

    private JsonNode emploiDuTemps(Decor d, LocalDate debut, LocalDate fin) throws Exception {
        var requete = avecJeton(get("/moi/emploi-du-temps"), d.jetonEtudiant);
        if (debut != null) {
            requete = requete.param("debut", debut.toString());
        }
        if (fin != null) {
            requete = requete.param("fin", fin.toString());
        }
        return donnees(mockMvc.perform(requete).andExpect(status().isOk()).andReturn());
    }

    private void planifier(Decor d, long classeId, LocalDate jour, int heure) throws Exception {
        creer("/seances", """
                {"classeId":%d,"matiereId":%d,"enseignantId":"%s","salleId":%d,
                 "debut":"%s","fin":"%s"}
                """.formatted(classeId, d.matiereId, d.enseignantId, d.salleId,
                jour.atTime(heure, 0).atZone(ZONE).toInstant(),
                jour.atTime(heure + 2, 0).atZone(ZONE).toInstant()));
    }

    private Decor preparer(String cle) throws Exception {
        long promotionId = creer("/promotions", """
                {"code":"PROMO-%s","libelle":"Promotion %s","anneeAcademique":2026}
                """.formatted(cle, cle)).get("id").asLong();

        long classeId = creer("/classes", """
                {"code":"CL-%s-1","libelle":"Classe A %s","promotionId":%d}
                """.formatted(cle, cle, promotionId)).get("id").asLong();
        long autreClasseId = creer("/classes", """
                {"code":"CL-%s-2","libelle":"Classe B %s","promotionId":%d}
                """.formatted(cle, cle, promotionId)).get("id").asLong();

        long matiereId = creer("/matieres", """
                {"code":"MAT-%s","libelle":"Matiere %s","credits":3}
                """.formatted(cle, cle)).get("id").asLong();
        long salleId = creer("/salles", """
                {"code":"S-%s","nom":"Salle %s","capacite":40}
                """.formatted(cle, cle)).get("id").asLong();

        String enseignantId = creer("/personnels", """
                {"matricule":"PROF-%s","nom":"Toure","prenom":"Ibrahim",
                 "type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();

        String etudiantId = creer("/etudiants", """
                {"matricule":"%s-ET","nom":"Cisse","prenom":"Awa","classeId":%d,"actif":true}
                """.formatted(cle, classeId)).get("id").asText();

        JsonNode compte = creer("/etudiants/" + etudiantId + "/compte", null);
        String jeton = seConnecter(compte.get("email").asText(),
                compte.get("motDePasseInitial").asText());

        return new Decor(classeId, autreClasseId, matiereId, salleId, enseignantId, jeton);
    }

    private JsonNode creer(String chemin, String corps) throws Exception {
        var requete = avecJeton(post(chemin), jetonAdmin);
        if (corps != null) {
            requete = requete.contentType(MediaType.APPLICATION_JSON).content(corps);
        }
        return donnees(mockMvc.perform(requete).andExpect(status().isCreated()).andReturn());
    }
}
