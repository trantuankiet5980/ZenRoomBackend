package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.NotificationDto;
import vn.edu.iuh.fit.entities.Notification;

@Component
@RequiredArgsConstructor
public class NotificationMapper {

    private final UserMapper userMapper;

    public NotificationDto toDto(Notification entity) {
        if (entity == null) return null;

        return new NotificationDto(
                entity.getNotificationId(),
                entity.getUser() != null ? userMapper.toDto(entity.getUser()) : null,
                entity.getTitle(),
                entity.getMessage(),
                entity.getType(),
                entity.getRedirectUrl(),
                entity.getIsRead(),
                entity.getCreatedAt()
        );
    }

    public Notification toEntity(NotificationDto dto) {
        if (dto == null) return null;

        Notification entity = new Notification();
        entity.setNotificationId(dto.getNotificationId());
        entity.setTitle(dto.getTitle());
        entity.setMessage(dto.getMessage());
        entity.setType(dto.getType());
        entity.setRedirectUrl(dto.getRedirectUrl());
        entity.setIsRead(dto.getIsRead());
        entity.setCreatedAt(dto.getCreatedAt());

        // User sẽ được set trong Service từ DB (userRepository.findById)
        return entity;
    }
}
