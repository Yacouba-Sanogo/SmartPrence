package com.smartpresence.config;

import com.smartpresence.constants.RoleCode;
import com.smartpresence.entity.ParametreEtablissement;
import com.smartpresence.entity.Role;
import com.smartpresence.entity.Utilisateur;
import com.smartpresence.repository.ParametreEtablissementRepository;
import com.smartpresence.repository.RoleRepository;
import com.smartpresence.repository.UtilisateurRepository;
import com.smartpresence.utils.GenerateurMotDePasse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Initialise les référentiels indispensables au démarrage : rôles applicatifs,
 * configuration de l'établissement et compte administrateur d'amorçage.
 *
 * <p>Chaque insertion est conditionnée à l'absence de la donnée : le démarrage est
 * <b>idempotent</b> et n'écrase jamais une configuration existante.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final ParametreEtablissementRepository parametreEtablissementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.bootstrap.email}")
    private String emailAmorcage;

    @Value("${app.security.bootstrap.password:}")
    private String motDePasseAmorcage;

    @Value("${app.security.bootstrap.nom}")
    private String nomAmorcage;

    @Value("${app.security.bootstrap.prenom}")
    private String prenomAmorcage;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedParametresEtablissement();
        seedAdministrateurAmorcage();
    }

    private void seedRoles() {
        seedRole(RoleCode.ADMIN, "Administrateur", "Accès complet au système");
        seedRole(RoleCode.ENSEIGNANT, "Enseignant", "Consultation et gestion des présences");
        seedRole(RoleCode.RESPONSABLE_SCOLARITE, "Responsable scolarité", "Gestion administrative des étudiants");
        seedRole(RoleCode.SUPERVISEUR, "Superviseur", "Supervision globale et indicateurs");
        seedRole(RoleCode.RH, "Ressources humaines",
                "Gestion du personnel, enrôlement biométrique et suivi des pointages");
        seedRole(RoleCode.PERSONNEL, "Personnel",
                "Consultation de ses propres pointages et de son cumul d'heures");
        seedRole(RoleCode.ETUDIANT, "Étudiant",
                "Consultation de ses propres présences, de son emploi du temps et de ses justificatifs");
    }

    private void seedRole(RoleCode code, String libelle, String description) {
        if (!roleRepository.existsByCode(code.name())) {
            Role role = new Role();
            role.setCode(code.name());
            role.setLibelle(libelle);
            role.setDescription(description);
            roleRepository.save(role);
            log.info("Rôle initialisé : {}", code.name());
        }
    }

    /**
     * Crée la ligne de configuration d'identifiant {@code 1} si elle est absente.
     *
     * <p>Sans elle, la consultation des paramètres échouerait en {@code 404} et le calcul
     * des retards du personnel n'aurait aucune heure d'ouverture de référence.</p>
     */
    private void seedParametresEtablissement() {
        if (parametreEtablissementRepository.existsById(1L)) {
            return;
        }
        ParametreEtablissement parametres = new ParametreEtablissement();
        parametres.setId(1L);
        parametres.setNom("Université SmartPresence");
        parametres.setSigle("USP");
        parametreEtablissementRepository.save(parametres);
        log.info("Paramètres de l'établissement initialisés (ouverture {}, seuil de retard {} min)",
                parametres.getHeureOuverture(), parametres.getSeuilRetardMinutes());
    }

    /**
     * Crée un administrateur d'amorçage lors du tout premier démarrage.
     *
     * <p>La condition est volontairement « <b>aucun</b> utilisateur en base » et non
     * « cet email n'existe pas » : une fois le parc de comptes constitué, ce mécanisme
     * ne doit plus jamais réintroduire un compte privilégié, y compris si
     * l'administrateur d'origine a été supprimé.</p>
     *
     * <p>Faute de mot de passe configuré, un mot de passe aléatoire est tiré et affiché
     * une seule fois au démarrage. Aucun identifiant par défaut connu n'existe donc,
     * à aucun moment.</p>
     */
    private void seedAdministrateurAmorcage() {
        if (utilisateurRepository.count() > 0) {
            return;
        }

        Role roleAdmin = roleRepository.findByCode(RoleCode.ADMIN.name())
                .orElseThrow(() -> new IllegalStateException(
                        "Le rôle ADMIN doit exister avant la création du compte d'amorçage"));

        boolean motDePasseGenere = !StringUtils.hasText(motDePasseAmorcage);
        String motDePasseClair = motDePasseGenere ? GenerateurMotDePasse.generer(16) : motDePasseAmorcage;

        Utilisateur administrateur = new Utilisateur();
        administrateur.setEmail(emailAmorcage);
        administrateur.setMotDePasse(passwordEncoder.encode(motDePasseClair));
        administrateur.setNom(nomAmorcage);
        administrateur.setPrenom(prenomAmorcage);
        administrateur.setActif(true);
        administrateur.setRoles(Set.of(roleAdmin));
        utilisateurRepository.save(administrateur);

        if (motDePasseGenere) {
            log.warn("""

                    ================================================================
                     COMPTE ADMINISTRATEUR D'AMORÇAGE CRÉÉ
                     Email          : {}
                     Mot de passe   : {}
                     Ce mot de passe aléatoire n'est affiché qu'une seule fois.
                     Notez-le, puis changez-le après la première connexion.
                    ================================================================
                    """, emailAmorcage, motDePasseClair);
        } else {
            log.info("Compte administrateur d'amorçage créé pour {} (mot de passe fourni par configuration)",
                    emailAmorcage);
        }
    }

}
