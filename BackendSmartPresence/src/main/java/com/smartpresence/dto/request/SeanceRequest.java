package com.smartpresence.dto.request;
import jakarta.validation.constraints.NotNull;
import lombok.Getter; import lombok.Setter;
import java.time.Instant; import java.util.UUID;
@Getter @Setter public class SeanceRequest { @NotNull private Long classeId; @NotNull private Long matiereId; @NotNull private UUID enseignantId; private Long salleId; @NotNull private Instant debut; @NotNull private Instant fin; private String note; }
