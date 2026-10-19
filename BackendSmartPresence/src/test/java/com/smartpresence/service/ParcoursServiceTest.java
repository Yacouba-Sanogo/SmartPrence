package com.smartpresence.service;

import com.smartpresence.constants.DecisionSemestre;
import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.dto.response.PassageResponse;
import com.smartpresence.dto.response.PassageResponse.Issue;
import com.smartpresence.dto.response.ReleveSemestreResponse;
import com.smartpresence.entity.Classe;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.entity.Filiere;
import com.smartpresence.entity.OptionFiliere;
import com.smartpresence.mapper.EtudiantMapper;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.OptionFiliereRepository;
import com.smartpresence.service.impl.ParcoursServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Règles du passage en année supérieure, sans base : les décisions de semestre sont
 * simulées, ce qui isole la logique de classe de destination.
 */
class ParcoursServiceTest {

    private final EtudiantRepository etudiants = mock(EtudiantRepository.class);
    private final ClasseRepository classes = mock(ClasseRepository.class);
    private final ReleveService releves = mock(ReleveService.class);
    private ParcoursService service;

    private Classe l2;
    private Classe l3Btp;
    private Classe l3Hydro;
    private OptionFiliere btp;
    private OptionFiliere hydro;

    @BeforeEach
    void preparer() {
        service = new ParcoursServiceImpl(etudiants, classes, mock(OptionFiliereRepository.class),
                releves, mock(EtudiantMapper.class));

        Filiere genieCivil = new Filiere();
        genieCivil.setId(1L);
        btp = option(1L, "BTP", genieCivil);
        hydro = option(2L, "HYDRO", genieCivil);

        l2 = classe(10L, "GC-L2", null);
        l3Btp = classe(11L, "GC-L3-BTP", btp);
        l3Hydro = classe(12L, "GC-L3-HYDRO", hydro);

        when(classes.findById(10L)).thenReturn(Optional.of(l2));
        when(classes.findByClassePrecedenteId(10L)).thenReturn(List.of(l3Btp, l3Hydro));
    }

    private static OptionFiliere option(Long id, String code, Filiere filiere) {
        OptionFiliere o = new OptionFiliere();
        o.setId(id);
        o.setCode(code);
        o.setLibelle(code);
        o.setFiliere(filiere);
        return o;
    }

    private static Classe classe(Long id, String code, OptionFiliere option) {
        Classe c = new Classe();
        c.setId(id);
        c.setCode(code);
        c.setOption(option);
        return c;
    }

    private Etudiant etudiant(String matricule, OptionFiliere choisie, DecisionSemestre... decisions) {
        Etudiant e = new Etudiant();
        e.setId(UUID.randomUUID());
        e.setMatricule(matricule);
        e.setNom(matricule);
        e.setPrenom("X");
        e.setActif(true);
        e.setClasse(l2);
        e.setOptionChoisie(choisie);

        List<PeriodeScolaire> semestres = List.of(PeriodeScolaire.values()).subList(0, decisions.length);
        for (int i = 0; i < decisions.length; i++) {
            ReleveSemestreResponse r = ReleveSemestreResponse.builder()
                    .semestre(semestres.get(i))
                    .semestresDisponibles(semestres)
                    .decision(decisions[i])
                    .build();
            when(releves.releveDe(e.getId(), semestres.get(i))).thenReturn(r);
            if (i == 0) {
                when(releves.releveDe(eq(e.getId()), eq(null))).thenReturn(r);
            }
        }
        return e;
    }

    private PassageResponse passage(boolean appliquer, Etudiant... presents) {
        when(etudiants.findByClasseId(10L)).thenReturn(List.of(presents));
        return service.passage(10L, appliquer);
    }

    @Test
    void ladmisRejointLaClasseDeSonOption() {
        Etudiant a = etudiant("A", btp, DecisionSemestre.VALIDE, DecisionSemestre.VALIDE);
        Etudiant b = etudiant("B", hydro, DecisionSemestre.VALIDE, DecisionSemestre.VALIDE);

        PassageResponse r = passage(true, a, b);

        assertEquals(2, r.promus());
        assertEquals(l3Btp, a.getClasse());
        assertEquals(l3Hydro, b.getClasse());
    }

    @Test
    void unSemestreNonValideFaitUnRedoublantQuiResteEnPlace() {
        Etudiant a = etudiant("A", btp, DecisionSemestre.VALIDE, DecisionSemestre.NON_VALIDE);

        PassageResponse r = passage(true, a);

        assertEquals(1, r.redoublants());
        assertEquals(l2, a.getClasse());
    }

    @Test
    void desNotesManquantesLaissentLEtudiantEnAttenteSansDecider() {
        Etudiant a = etudiant("A", btp, DecisionSemestre.VALIDE, DecisionSemestre.EN_ATTENTE);

        PassageResponse r = passage(true, a);

        assertEquals(1, r.enAttente());
        assertEquals(l2, a.getClasse());
    }

    @Test
    void sansOptionChoisieLEtudiantAdmisNeMontePasEtOnLeSignale() {
        Etudiant a = etudiant("A", null, DecisionSemestre.VALIDE);

        PassageResponse r = passage(true, a);

        assertEquals(1, r.optionsManquantes());
        assertEquals(Issue.OPTION_A_CHOISIR, r.lignes().get(0).issue());
        assertEquals(l2, a.getClasse());
    }

    @Test
    void uneSimulationNeDeplaceRien() {
        Etudiant a = etudiant("A", btp, DecisionSemestre.VALIDE);

        PassageResponse r = passage(false, a);

        assertEquals(1, r.promus());
        assertEquals("GC-L3-BTP", r.lignes().get(0).classeDestination());
        assertEquals(l2, a.getClasse());
        verify(etudiants, never()).save(any());
    }

    @Test
    void uneSeuleClasseSuperieureSuffitSansOption() {
        when(classes.findByClassePrecedenteId(10L)).thenReturn(List.of(l3Btp));
        Etudiant a = etudiant("A", null, DecisionSemestre.VALIDE);

        PassageResponse r = passage(true, a);

        assertEquals(1, r.promus());
        assertEquals(l3Btp, a.getClasse());
    }

    @Test
    void sansClasseSuperieureLAdmisEstSignaleTermineEtNeBougePas() {
        when(classes.findByClassePrecedenteId(10L)).thenReturn(List.of());
        Etudiant a = etudiant("A", null, DecisionSemestre.VALIDE);

        PassageResponse r = passage(true, a);

        assertEquals(1, r.termines());
        assertEquals(l2, a.getClasse());
    }
}
