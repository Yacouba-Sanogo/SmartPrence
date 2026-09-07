package com.smartpresence.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter; import lombok.Setter;
@Getter @Setter public class MatiereRequest { @NotBlank private String code; @NotBlank private String libelle; private Integer credits; private String description; private Boolean active; }
