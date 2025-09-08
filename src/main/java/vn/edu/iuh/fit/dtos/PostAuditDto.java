package vn.edu.iuh.fit.dtos;

import lombok.*;
import vn.edu.iuh.fit.entities.enums.PostAuditAction;
import vn.edu.iuh.fit.entities.enums.PostStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.PostAudit}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostAuditDto implements Serializable {
    String auditId;
    PostDto post;
    PostAuditAction action;
    PostStatus fromStatus;
    PostStatus toStatus;
    String reason;
    UserDto actor;
    LocalDateTime createdAt;
}