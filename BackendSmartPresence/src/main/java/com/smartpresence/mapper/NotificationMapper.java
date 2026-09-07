package com.smartpresence.mapper;

import com.smartpresence.dto.response.NotificationResponse;
import com.smartpresence.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * Mapper MapStruct pour l'entité {@link Notification}.
 *
 * @since 0.0.1
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificationMapper {

    @Mapping(target = "destinataireId", source = "destinataire.id")
    NotificationResponse toResponse(Notification entity);

    List<NotificationResponse> toResponseList(List<Notification> entities);
}
