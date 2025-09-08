package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.PostAuditDto;
import vn.edu.iuh.fit.entities.PostAudit;

@Component
@RequiredArgsConstructor
public class PostAuditMapper {

    private final PostMapper postMapper;
    private final UserMapper userMapper;

    public PostAuditDto toDto(PostAudit entity) {
        if (entity == null) return null;

        return PostAuditDto.builder()
                .auditId(entity.getAuditId())
                .post(entity.getPost() != null ? postMapper.toDTO(entity.getPost()) : null)
                .action(entity.getAction())
                .fromStatus(entity.getFromStatus())
                .toStatus(entity.getToStatus())
                .reason(entity.getReason())
                .actor(entity.getActor() != null ? userMapper.toDto(entity.getActor()) : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public PostAudit toEntity(PostAuditDto dto) {
        if (dto == null) return null;

        PostAudit entity = new PostAudit();
        entity.setAuditId(dto.getAuditId());
        entity.setAction(dto.getAction());
        entity.setFromStatus(dto.getFromStatus());
        entity.setToStatus(dto.getToStatus());
        entity.setReason(dto.getReason());
        entity.setCreatedAt(dto.getCreatedAt());

        // post và actor sẽ được gán trong Service bằng cách fetch từ DB (tránh vòng lặp/lazy)
        return entity;
    }
}
