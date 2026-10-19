package com.smartpresence.config;

import com.smartpresence.entity.Departement;
import com.smartpresence.entity.Filiere;
import com.smartpresence.entity.OptionFiliere;
import com.smartpresence.repository.DepartementRepository;
import com.smartpresence.repository.FiliereRepository;
import com.smartpresence.repository.OptionFiliereRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Amorce le référentiel de l'ENETP : quatre DER, cinq filières et leurs options.
 *
 * <p>Idempotent : chaque ligne n'est créée que si son code est absent, de sorte qu'un
 * ajout ou une modification faits par l'administration ne sont jamais écrasés.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReferentielEnetpInitializer implements CommandLineRunner {

    private final DepartementRepository departementRepository;
    private final FiliereRepository filiereRepository;
    private final OptionFiliereRepository optionRepository;

    private record Opt(String code, String libelle) {
    }

    @Override
    @Transactional
    public void run(String... args) {
        Departement bureautique = der("DER-BC", "DER Bureautique Communication");
        Departement comptabilite = der("DER-CG", "DER Comptabilité Gestion");
        Departement genieCivil = der("DER-BTP-GC", "DER Bâtiments et Travaux Publics / Génie Civil");
        Departement mecanique = der("DER-GMEM", "DER Génie Mécanique, Énergétique et Mines");

        filiere("BUREAUTIQUE-COM", "Bureautique Communication", bureautique, List.of(
                new Opt("BC-AES", "Administration Économique et Sociale"),
                new Opt("BC-AP", "Administration Publique"),
                new Opt("BC-GRH", "Gestion des Ressources Humaines (GRH)"),
                new Opt("BC-IC", "Information et Communication")));

        filiere("COMPTA-GESTION", "Comptabilité Gestion", comptabilite, List.of(
                new Opt("CG-COMPTA", "Comptabilité"),
                new Opt("CG-FINANCE", "Finance"),
                new Opt("CG-FISCALITE", "Fiscalité"),
                new Opt("CG-TA", "Techniques Administratives")));

        filiere("GENIE-CIVIL", "Génie Civil", genieCivil, List.of(
                new Opt("GC-BTP", "Bâtiments et Travaux Publics (BTP)"),
                new Opt("GC-HYDRO", "Hydraulique")));

        filiere("GENIE-INDUSTRIEL", "Génie Industriel", mecanique, List.of(
                new Opt("GI-ELECTROTECH", "Électrotechnique"),
                new Opt("GI-ELECTRONIQUE", "Électronique"),
                new Opt("GI-MECANIQUE", "Mécanique"),
                new Opt("GI-ENERGETIQUE", "Énergétique")));

        filiere("MINES", "Mines", mecanique, List.of(
                new Opt("MI-EXPLOITATION", "Exploitation Minière"),
                new Opt("MI-GEOTECHNIQUE", "Génie Géotechnique")));
    }

    private Departement der(String code, String libelle) {
        return departementRepository.findByCode(code).orElseGet(() -> {
            Departement d = new Departement();
            d.setCode(code);
            d.setLibelle(libelle);
            log.info("Référentiel ENETP : département {} ajouté", code);
            return departementRepository.save(d);
        });
    }

    private void filiere(String code, String libelle, Departement departement, List<Opt> options) {
        Filiere filiere = filiereRepository.findByCode(code).orElseGet(() -> {
            Filiere f = new Filiere();
            f.setCode(code);
            f.setLibelle(libelle);
            f.setDepartement(departement);
            log.info("Référentiel ENETP : filière {} ajoutée", code);
            return filiereRepository.save(f);
        });
        for (Opt opt : options) {
            if (!optionRepository.existsByCode(opt.code())) {
                OptionFiliere o = new OptionFiliere();
                o.setCode(opt.code());
                o.setLibelle(opt.libelle());
                o.setFiliere(filiere);
                optionRepository.save(o);
            }
        }
    }
}
