package com.smartpresence.mapper;

import com.smartpresence.dto.response.RoleResponse;
import com.smartpresence.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;
import java.util.Set;

/**
 * Mapper MapStruct pour l'entité {@link Role}.
 *
 * @since 0.0.1
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RoleMapper {

    RoleResponse toResponse(Role entity);

    List<RoleResponse> toResponseList(List<Role> entities);

    Set<RoleResponse> toResponseSet(Set<Role> entities);
}
