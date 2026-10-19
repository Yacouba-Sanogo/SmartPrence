package com.smartpresence.controller;

import com.smartpresence.dto.request.EligibiliteRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.EligibiliteResponse;
import com.smartpresence.dto.response.RegleConcoursResponse;
import com.smartpresence.service.ConcoursService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Concours d'entrée : adresses publiques, un candidat n'a pas de compte. */
@RestController
@RequestMapping("/concours")
@RequiredArgsConstructor
@Tag(name = "Concours", description = "Éligibilité au concours d'entrée à l'ENETP")
public class ConcoursController {

    private final ConcoursService concoursService;

    @Operation(summary = "Limites d'âge par diplôme")
    @GetMapping("/regles")
    public ResponseEntity<ApiResponse<List<RegleConcoursResponse>>> regles() {
        return ResponseEntity.ok(ApiResponse.success(concoursService.regles()));
    }

    @Operation(summary = "Savoir si l'on peut se présenter au concours",
            description = "Ne conserve rien : le calcul porte sur la seule date de naissance "
                    + "et le diplôme transmis.")
    @PostMapping("/eligibilite")
    public ResponseEntity<ApiResponse<EligibiliteResponse>> verifier(
            @Valid @RequestBody EligibiliteRequest requete) {
        return ResponseEntity.ok(ApiResponse.success(concoursService.verifier(requete)));
    }
}
