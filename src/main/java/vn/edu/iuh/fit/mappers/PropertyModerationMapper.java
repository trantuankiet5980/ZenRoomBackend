package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.PropertyModerationDto;
import vn.edu.iuh.fit.entities.PropertyModeration;

@Component
@RequiredArgsConstructor
public class PropertyModerationMapper {

    private final PropertyMapper propertyMapper;
    private final UserMapper userMapper;

    /** Entity -> DTO */
    public PropertyModerationDto toDto(PropertyModeration e) {
        if (e == null) return null;
        return new PropertyModerationDto(
                e.getId(),
                propertyMapper.toDto(e.getProperty()),
                e.getAction(),
                e.getFromStatus(),
                e.getToStatus(),
                e.getReason(),
                userMapper.toDto(e.getActor()),
                e.getCreatedAt()
        );
    }

    /** DTO -> Entity (ít dùng, thường chỉ cần toDto) */
    public PropertyModeration toEntity(PropertyModerationDto d) {
        if (d == null) return null;
        return PropertyModeration.builder()
                .id(d.getId())
                .property(propertyMapper.toEntity(d.getProperty()))
                .action(d.getAction())
                .fromStatus(d.getFromStatus())
                .toStatus(d.getToStatus())
                .reason(d.getReason())
                .actor(userMapper.toEntity(d.getActor()))
                .createdAt(d.getCreatedAt())
                .build();
    }
}
