package com.smartpresence.mapper;

import com.smartpresence.dto.request.UserRegistrationRequest;
import com.smartpresence.dto.request.UserUpdateRequest;
import com.smartpresence.dto.response.UserResponse;
import com.smartpresence.entity.Utilisateur;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * Mapper MapStruct pour l'entité {@link Utilisateur}.
 *
 * @since 0.0.1
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {RoleMapper.class})
public interface UtilisateurMapper {

    UserResponse toResponse(Utilisateur entity);

    List<UserResponse> toResponseList(List<Utilisateur> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true) // Géré par le service (conversion String -> Role)
    @Mapping(target = "motDePasse", ignore = true) // Géré par le service (hachage BCrypt)
    @Mapping(target = "actif", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Utilisateur toEntity(UserRegistrationRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "motDePasse", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UserUpdateRequest request, @MappingTarget Utilisateur entity);
}
