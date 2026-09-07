package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Notes et bulletins.
 *
 * <p>Deux choses se jouent ici : qu'un enseignant ne puisse noter que dans ses propres
 * classes, et que les moyennes soient justes. La première protège les résultats, la
 * seconde fait qu'on peut s'y fier.</p>
 */
@DisplayName("Intégration — notes et bulletin")
class NoteBulletinTest extends IntegrationTestBase {

    @Test
    @DisplayName("la moyenne d'une matière est pondérée par les coefficients")
    void moyennePondereeParMatiere() throws Exception {
        Decor d = preparer("NOT-A");

        // 8 coefficient 1, 14 coefficient 3 → (8 + 42) / 4 = 12,50.
        // Une moyenne simple donnerait 11, ce que le test doit écarter.
        saisir(d, d.etudiantId, d.matiereId, "8.00", 1, "Devoir 1");
        saisir(d, d.etudiantId, d.matiereId, "14.00", 3, "Composition");

        JsonNode bulletin = bulletinEtudiant(d);
        JsonNode matiere = bulletin.get("matieres").get(0);

        assertThat(matiere.get("moyenne").asDouble()).isEqualTo(12.50);
        assertThat(bulletin.get("nombreNotes").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("la moyenne générale pondère les matières par leurs crédits")
    void moyenneGeneralePondereeParCredits() throws Exception {
        Decor d = preparer("NOT-B");

        // Matière A : 4 crédits, moyenne 15. Matière B : 1 crédit, moyenne 5.
        // (15×4 + 5×1) / 5 = 13,00 — et non 10, qui serait la moyenne des moyennes.
        saisir(d, d.etudiantId, d.matiereId, "15.00", 1, "Devoir A");
        saisir(d, d.etudiantId, d.matiereSecondaireId, "5.00", 1, "Devoir B");

        JsonNode bulletin = bulletinEtudiant(d);
        assertThat(bulletin.get("moyenneGenerale").asDouble()).isEqualTo(13.00);
    }

    @Test
    @DisplayName("un étudiant sans note n'a pas de moyenne, et non zéro")
    void aucuneNoteDonneUneMoyenneNulle() throws Exception {
        Decor d = preparer("NOT-C");

        JsonNode bulletin = bulletinEtudiant(d);
        assertThat(bulletin.get("moyenneGenerale").isNull())
                .as("zéro afficherait un échec là où il n'y a aucune évaluation")
                .isTrue();
        assertThat(bulletin.get("nombreNotes").asInt()).isZero();
        assertThat(bulletin.get("matieres")).isEmpty();
    }

    @Test
    @DisplayName("une matière sans note ne fait pas chuter la moyenne générale")
    void matiereSansNoteEstEcartee() throws Exception {
        Decor d = preparer("NOT-D");

        // Seule la matière principale est notée ; la seconde reste vierge.
        saisir(d, d.etudiantId, d.matiereId, "16.00", 1, "Devoir");

        JsonNode bulletin = bulletinEtudiant(d);
        assertThat(bulletin.get("moyenneGenerale").asDouble()).isEqualTo(16.00);
        assertThat(bulletin.get("matieres")).hasSize(1);
    }

    @Test
    @DisplayName("un enseignant ne peut pas noter dans une classe où il n'intervient pas")
    void notationHorsDeSesClassesRefusee() throws Exception {
        Decor sien = preparer("NOT-E");
        Decor autre = preparer("NOT-F");

        mockMvc.perform(avecJeton(post("/moi/notes"), sien.jetonEnseignant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsNote(autre.etudiantId, sien.matiereId,
                                "18.00", 1, "Note frauduleuse")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un enseignant ne peut pas modifier la note d'un collègue")
    void noteDUnCollegueEstProtegee() throws Exception {
        Decor d = preparer("NOT-G");
        String noteId = donnees(saisir(d, d.etudiantId, d.matiereId, "9.00", 1, "Devoir"))
                .get("id").asText();

        // Le second enseignant intervient dans la même classe, mais n'est pas l'auteur.
        mockMvc.perform(avecJeton(put("/moi/notes/" + noteId), d.jetonSecondEnseignant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsNote(d.etudiantId, d.matiereId, "19.00", 1, "Corrigee")))
                .andExpect(status().isForbidden());

        mockMvc.perform(avecJeton(delete("/moi/notes/" + noteId), d.jetonSecondEnseignant))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("l'enseignant corrige et supprime ses propres notes")
    void correctionParLAuteur() throws Exception {
        Decor d = preparer("NOT-H");
        String noteId = donnees(saisir(d, d.etudiantId, d.matiereId, "9.00", 1, "Devoir"))
                .get("id").asText();

        MvcResult corrigee = mockMvc.perform(avecJeton(put("/moi/notes/" + noteId),
                        d.jetonEnseignant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsNote(d.etudiantId, d.matiereId, "13.50", 2, "Devoir revu")))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(donnees(corrigee).get("valeur").asDouble()).isEqualTo(13.50);
        assertThat(donnees(corrigee).get("coefficient").asInt()).isEqualTo(2);

        mockMvc.perform(avecJeton(delete("/moi/notes/" + noteId), d.jetonEnseignant))
                .andExpect(status().isOk());
        assertThat(bulletinEtudiant(d).get("nombreNotes").asInt()).isZero();
    }

    @Test
    @DisplayName("une note hors du barème est refusée")
    void noteHorsBaremeRefusee() throws Exception {
        Decor d = preparer("NOT-I");

        mockMvc.perform(avecJeton(post("/moi/notes"), d.jetonEnseignant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsNote(d.etudiantId, d.matiereId, "21.00", 1, "Trop haute")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(avecJeton(post("/moi/notes"), d.jetonEnseignant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsNote(d.etudiantId, d.matiereId, "-1.00", 1, "Negative")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("un étudiant ne voit que ses propres notes")
    void bulletinCloisonne() throws Exception {
        Decor d = preparer("NOT-J");
        saisir(d, d.etudiantId, d.matiereId, "17.00", 1, "Devoir");
        saisir(d, d.etudiantSecondaireId, d.matiereId, "6.00", 1, "Devoir");

        JsonNode bulletin = bulletinEtudiant(d);
        assertThat(bulletin.get("nombreNotes").asInt()).isEqualTo(1);
        assertThat(bulletin.get("matieres").get(0).get("notes").get(0).get("valeur").asDouble())
                .isEqualTo(17.00);

        // La route de saisie reste fermée à l'étudiant.
        mockMvc.perform(avecJeton(post("/moi/notes"), d.jetonEtudiant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsNote(d.etudiantId, d.matiereId, "20.00", 1, "Auto-notation")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("l'enseignant voit les notes de sa classe, filtrées par matière")
    void notesDeLaClasse() throws Exception {
        Decor d = preparer("NOT-K");
        saisir(d, d.etudiantId, d.matiereId, "11.00", 1, "Devoir");
        saisir(d, d.etudiantSecondaireId, d.matiereId, "15.00", 1, "Devoir");
        saisir(d, d.etudiantId, d.matiereSecondaireId, "8.00", 1, "Devoir B");

        JsonNode toutes = donnees(mockMvc.perform(avecJeton(
                        get("/moi/classes/" + d.classeId + "/notes"), d.jetonEnseignant))
                .andExpect(status().isOk()).andReturn());
        assertThat(toutes).hasSize(3);

        JsonNode filtrees = donnees(mockMvc.perform(avecJeton(
                        get("/moi/classes/" + d.classeId + "/notes")
                                .param("matiereId", String.valueOf(d.matiereId)),
                        d.jetonEnseignant))
                .andExpect(status().isOk()).andReturn());
        assertThat(filtrees).hasSize(2);
    }

    @Test
    @DisplayName("le bulletin se filtre par période")
    void bulletinParPeriode() throws Exception {
        Decor d = preparer("NOT-L");
        saisirPeriode(d, d.matiereId, "10.00", "SEMESTRE_1");
        saisirPeriode(d, d.matiereId, "18.00", "SEMESTRE_2");

        assertThat(bulletinEtudiant(d).get("nombreNotes").asInt()).isEqualTo(2);

        JsonNode s2 = donnees(mockMvc.perform(avecJeton(
                        get("/moi/bulletin").param("periode", "SEMESTRE_2"), d.jetonEtudiant))
                .andExpect(status().isOk()).andReturn());
        assertThat(s2.get("nombreNotes").asInt()).isEqualTo(1);
        assertThat(s2.get("moyenneGenerale").asDouble()).isEqualTo(18.00);
    }

    // ------------------------------------------------------------------

    private record Decor(long classeId, long matiereId, long matiereSecondaireId,
                         String etudiantId, String etudiantSecondaireId,
                         String jetonEnseignant, String jetonSecondEnseignant,
                         String jetonEtudiant) {}

    private MvcResult saisir(Decor d, String etudiantId, long matiereId,
                             String valeur, int coefficient, String libelle) throws Exception {
        return mockMvc.perform(avecJeton(post("/moi/notes"), d.jetonEnseignant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsNote(etudiantId, matiereId, valeur, coefficient, libelle)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private void saisirPeriode(Decor d, long matiereId, String valeur, String periode)
            throws Exception {
        mockMvc.perform(avecJeton(post("/moi/notes"), d.jetonEnseignant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etudiantId":"%s","matiereId":%d,"valeur":%s,"coefficient":1,
                                 "libelle":"Devoir","dateEvaluation":"%s","periode":"%s"}
                                """.formatted(d.etudiantId, matiereId, valeur,
                                LocalDate.now(), periode)))
                .andExpect(status().isCreated());
    }

    private String corpsNote(String etudiantId, long matiereId, String valeur,
                             int coefficient, String libelle) {
        return """
                {"etudiantId":"%s","matiereId":%d,"valeur":%s,"coefficient":%d,
                 "libelle":"%s","dateEvaluation":"%s","type":"DEVOIR"}
                """.formatted(etudiantId, matiereId, valeur, coefficient, libelle,
                LocalDate.now());
    }

    private JsonNode bulletinEtudiant(Decor d) throws Exception {
        return donnees(mockMvc.perform(avecJeton(get("/moi/bulletin"), d.jetonEtudiant))
                .andExpect(status().isOk())
                .andReturn());
    }

    private Decor preparer(String cle) throws Exception {
        long promotionId = creer("/promotions", """
                {"code":"PROMO-%s","libelle":"Promotion %s","anneeAcademique":2026}
                """.formatted(cle, cle)).get("id").asLong();

        long classeId = creer("/classes", """
                {"code":"%s","libelle":"Classe %s","promotionId":%d}
                """.formatted(cle, cle, promotionId)).get("id").asLong();

        String enseignantId = creer("/personnels", """
                {"matricule":"PROF-%s-1","nom":"Diallo","prenom":"Awa","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();
        String secondEnseignantId = creer("/personnels", """
                {"matricule":"PROF-%s-2","nom":"Sow","prenom":"Modibo","type":"ENSEIGNANT","actif":true}
                """.formatted(cle)).get("id").asText();

        mockMvc.perform(avecJeton(put("/classes/" + classeId), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","libelle":"Classe %s","promotionId":%d,
                                 "enseignantIds":["%s","%s"]}
                                """.formatted(cle, cle, promotionId, enseignantId,
                                secondEnseignantId)))
                .andExpect(status().isOk());

        // Crédits différents : c'est ce qui rend la pondération de la générale visible.
        long matiereId = creer("/matieres", """
                {"code":"MAT-%s-A","libelle":"Matiere A %s","credits":4}
                """.formatted(cle, cle)).get("id").asLong();
        long matiereSecondaireId = creer("/matieres", """
                {"code":"MAT-%s-B","libelle":"Matiere B %s","credits":1}
                """.formatted(cle, cle)).get("id").asLong();

        String etudiantId = creer("/etudiants", """
                {"matricule":"%s-ET1","nom":"Keita","prenom":"Sira","classeId":%d,"actif":true}
                """.formatted(cle, classeId)).get("id").asText();
        String etudiantSecondaireId = creer("/etudiants", """
                {"matricule":"%s-ET2","nom":"Coulibaly","prenom":"Bakary","classeId":%d,"actif":true}
                """.formatted(cle, classeId)).get("id").asText();

        return new Decor(classeId, matiereId, matiereSecondaireId,
                etudiantId, etudiantSecondaireId,
                jetonDe("/personnels/" + enseignantId + "/compte"),
                jetonDe("/personnels/" + secondEnseignantId + "/compte"),
                jetonDe("/etudiants/" + etudiantId + "/compte"));
    }

    /** Ouvre un accès et retourne le jeton correspondant. */
    private String jetonDe(String cheminCompte) throws Exception {
        JsonNode compte = creer(cheminCompte, null);
        return seConnecter(compte.get("email").asText(),
                compte.get("motDePasseInitial").asText());
    }

    private JsonNode creer(String chemin, String corps) throws Exception {
        var requete = avecJeton(post(chemin), jetonAdmin);
        if (corps != null) {
            requete = requete.contentType(MediaType.APPLICATION_JSON).content(corps);
        }
        return donnees(mockMvc.perform(requete).andExpect(status().isCreated()).andReturn());
    }
}
