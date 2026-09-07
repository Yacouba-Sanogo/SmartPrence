package com.smartpresence.dto.request;
import com.smartpresence.constants.TypePersonnel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter; import lombok.Setter;
import java.util.UUID;
@Getter @Setter public class PersonnelRequest { @NotBlank private String matricule; @NotBlank private String nom; @NotBlank private String prenom; private String email; private String telephone; @NotNull private TypePersonnel type; private String service; private UUID utilisateurId; private Boolean actif; }
