package com.smartpresence.dto.response;
import com.smartpresence.constants.StatutSeance;
import lombok.Builder; import lombok.Getter;
import java.time.Instant; import java.util.UUID;
@Getter @Builder public class SeanceResponse { private UUID id; private Long classeId; private String classeCode; private Long matiereId; private String matiereLibelle; private UUID enseignantId; private String enseignantNom; private Long salleId; private String salleLibelle; private Instant debut; private Instant fin; private StatutSeance statut; private String note; }
