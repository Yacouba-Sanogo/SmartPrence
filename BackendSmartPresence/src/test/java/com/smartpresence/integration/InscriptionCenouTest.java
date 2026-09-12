package com.smartpresence.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Intégration — inscription libre des étudiants, adossée au référentiel CENOU.
 *
 * <p>Deux propriétés se vérifient ici, et aucune ne se voit à la compilation : que
 * le référentiel <b>ferme</b> réellement l'inscription à qui n'y figure pas, et
 * qu'un import de tableur ordinaire — avec sa ligne d'en-tête, ses doublons et ses
 * cellules vides — aboutisse au lieu d'échouer en bloc.</p>
 */
@DisplayName("Intégration — inscription libre et référentiel CENOU")
class InscriptionCenouTest extends IntegrationTestBase {

    @Test
    @DisplayName("sans numéro au référentiel, l'inscription est refusée")
    void inscriptionRefuseeSansNumeroAuReferentiel() throws Exception {
        long classeId = classeDeTest();

        mockMvc.perform(post("/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numeroCenou":"CEN-INCONNU-9999","nom":"Diallo","prenom":"Aissata",
                                 "classeId":%d,"motDePasse":"MotDePasse2026"}
                                """.formatted(classeId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("la liste des classes est consultable sans aucun jeton")
    void listeDesClassesEstPublique() throws Exception {
        classeDeTest();

        // Aucun en-tête d'autorisation : un candidat n'a pas encore de compte.
        MvcResult resultat = mockMvc.perform(get("/inscription/classes"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(donnees(resultat).isArray()).isTrue();
        assertThat(donnees(resultat)).isNotEmpty();
    }

    @Test
    @DisplayName("un import CSV enregistre les lignes valides et rend compte des autres")
    void importCsvToleranteAuxScoriesHabituelles() throws Exception {
        String csv = """
                Numero CENOU;Nom;Prenom
                CEN-CSV-1001;SIDIBE;Aminata
                CEN-CSV-1002;DEMBELE;Seydou
                cen-csv-1002;DEMBELE;Seydou
                ;SANS;Numero
                CEN-CSV-1003;COULIBALY;Mariam
                """;

        MvcResult resultat = mockMvc.perform(avecJeton(
                        multipart("/cenou/import").file(new MockMultipartFile(
                                "fichier", "liste.csv", "text/csv",
                                csv.getBytes(StandardCharsets.UTF_8))),
                        jetonAdmin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode compteRendu = donnees(resultat);
        assertThat(compteRendu.get("lignesLues").asInt()).isEqualTo(5);
        assertThat(compteRendu.get("ajoutes").asInt()).isEqualTo(3);
        assertThat(compteRendu.get("doublons").asInt())
                .as("« cen-csv-1002 » et « CEN-CSV-1002 » désignent le même étudiant")
                .isEqualTo(1);
        assertThat(compteRendu.get("rejets")).hasSize(1);
        assertThat(compteRendu.get("rejets").get(0).asText()).contains("numéro absent");
    }

    @Test
    @DisplayName("un classeur .xlsx est lu, en-tête comprise")
    void importTableurLitUnVraiClasseur() throws Exception {
        byte[] classeur = classeurDeTest("CEN-XLS-2001", "CEN-XLS-2002");

        MvcResult resultat = mockMvc.perform(avecJeton(
                        multipart("/cenou/import").file(new MockMultipartFile(
                                "fichier", "liste.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                classeur)),
                        jetonAdmin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode compteRendu = donnees(resultat);
        assertThat(compteRendu.get("lignesLues").asInt())
                .as("la ligne d'en-tête ne doit pas être comptée")
                .isEqualTo(2);
        assertThat(compteRendu.get("ajoutes").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("le numéro importé ouvre l'inscription, une fois et une seule")
    void inscriptionAboutitPuisLeNumeroEstConsomme() throws Exception {
        long classeId = classeDeTest();
        long idReference = ajouterNumero("CEN-UNIQUE-3001");

        String demande = """
                {"numeroCenou":"cen-unique-3001","nom":"Keita","prenom":"Salif",
                 "classeId":%d,"motDePasse":"MotDePasse2026"}
                """.formatted(classeId);

        MvcResult inscription = mockMvc.perform(post("/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demande))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode etudiant = donnees(inscription);
        assertThat(etudiant.get("matricule").asText())
                .as("le numéro CENOU devient le matricule, sous sa forme canonique")
                .isEqualTo("CEN-UNIQUE-3001");
        assertThat(etudiant.get("classeId").asLong()).isEqualTo(classeId);

        // L'étudiant doit pouvoir se connecter aussitôt, avec le mot de passe choisi.
        String jeton = seConnecter("cen-unique-3001@etudiant.smartpresence.local", "MotDePasse2026");
        MvcResult profil = mockMvc.perform(avecJeton(get("/moi/profil"), jeton))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(donnees(profil).get("typeProfil").asText()).isEqualTo("ETUDIANT");

        // Le même numéro ne peut plus servir…
        mockMvc.perform(post("/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demande))
                .andExpect(status().isConflict());

        // …ni être retiré du référentiel, ce qui rouvrirait la porte.
        mockMvc.perform(avecJeton(delete("/cenou/" + idReference), jetonAdmin))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------

    /** Ajoute un numéro au référentiel et retourne son identifiant. */
    private long ajouterNumero(String numero) throws Exception {
        MvcResult resultat = mockMvc.perform(avecJeton(post("/cenou"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numero":"%s","nom":"Keita","prenom":"Salif"}
                                """.formatted(numero)))
                .andExpect(status().isCreated())
                .andReturn();
        return donnees(resultat).get("id").asLong();
    }

    /** Classeur minimal : une ligne d'en-tête, puis un numéro par ligne. */
    private byte[] classeurDeTest(String... numeros) throws Exception {
        try (Workbook classeur = new XSSFWorkbook();
             ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Sheet feuille = classeur.createSheet("CENOU");
            Row entete = feuille.createRow(0);
            entete.createCell(0).setCellValue("Numero CENOU");
            entete.createCell(1).setCellValue("Nom");
            entete.createCell(2).setCellValue("Prenom");

            for (int i = 0; i < numeros.length; i++) {
                Row ligne = feuille.createRow(i + 1);
                ligne.createCell(0).setCellValue(numeros[i]);
                ligne.createCell(1).setCellValue("TOURE");
                ligne.createCell(2).setCellValue("Oumar");
            }
            classeur.write(sortie);
            return sortie.toByteArray();
        }
    }

    /**
     * Classe de rattachement, créée avec sa promotion au premier appel.
     *
     * <p>Un étudiant ne peut pas exister sans classe : la hiérarchie doit être
     * complète avant la moindre inscription.</p>
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
                                {"code":"PROMO-CENOU","libelle":"Promotion d'essai","anneeAcademique":2026}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long promotionId = donnees(promotion).get("id").asLong();

        MvcResult classe = mockMvc.perform(avecJeton(post("/classes"), jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"CL-CENOU","libelle":"Classe d'essai","promotionId":%d}
                                """.formatted(promotionId)))
                .andExpect(status().isCreated())
                .andReturn();
        return donnees(classe).get("id").asLong();
    }
}
