package com.smartpresence.dto.response;
import lombok.Builder; import lombok.Getter;
@Getter @Builder public class MatiereResponse { private Long id; private String code; private String libelle; private Integer credits; private String description; private boolean active;
    private Long uniteEnseignementId; private String uniteEnseignementCode;
    private String uniteEnseignementLibelle; }
