package com.smartpresence.service;

import com.smartpresence.constants.DiplomeCandidat;
import com.smartpresence.dto.request.EligibiliteRequest;
import com.smartpresence.dto.response.EligibiliteResponse;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.service.impl.ConcoursServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcoursServiceTest {

    private final ConcoursService service = new ConcoursServiceImpl();
    private static final LocalDate REFERENCE = LocalDate.of(2026, 10, 1);

    private EligibiliteResponse verifier(LocalDate naissance, DiplomeCandidat diplome) {
        return service.verifier(new EligibiliteRequest(naissance, diplome, REFERENCE));
    }

    @Test
    void bachelierAVingtDeuxAnsRevolusEstEligible() {
        EligibiliteResponse r = verifier(LocalDate.of(2004, 10, 1), DiplomeCandidat.BACCALAUREAT);
        assertEquals(22, r.age());
        assertTrue(r.eligible());
        assertEquals(4, r.piecesAFournir().size());
    }

    @Test
    void bachelierAVingtTroisAnsEstRefuseSansListeDePieces() {
        EligibiliteResponse r = verifier(LocalDate.of(2003, 9, 30), DiplomeCandidat.BACCALAUREAT);
        assertEquals(23, r.age());
        assertFalse(r.eligible());
        assertTrue(r.piecesAFournir().isEmpty());
    }

    @Test
    void licencieAVingtCinqAnsEstEligibleMaisPasAVingtSix() {
        assertTrue(verifier(LocalDate.of(2001, 10, 1), DiplomeCandidat.LICENCE).eligible());
        assertFalse(verifier(LocalDate.of(2000, 9, 30), DiplomeCandidat.LICENCE).eligible());
    }

    @Test
    void leProfessionnelDoitAussiFournirUneAutorisationDeLaFonctionPublique() {
        EligibiliteResponse r = verifier(LocalDate.of(1996, 10, 1), DiplomeCandidat.PROFESSIONNEL);
        assertTrue(r.eligible());
        assertEquals(5, r.piecesAFournir().size());
    }

    @Test
    void uneNaissanceApresLaDateDeReferenceEstRefusee() {
        assertThrows(BusinessException.class,
                () -> verifier(LocalDate.of(2027, 1, 1), DiplomeCandidat.LICENCE));
    }
}
