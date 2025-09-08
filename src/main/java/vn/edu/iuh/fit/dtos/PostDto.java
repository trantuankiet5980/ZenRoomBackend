package vn.edu.iuh.fit.dtos;

import lombok.*;
import vn.edu.iuh.fit.entities.enums.PostStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Post}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostDto implements Serializable {
    String postId;
    UserDto landlord;
    PropertyDto property;
    String title;
    String description;
    Boolean isFireSafe;
    String contactPhone;
    PostStatus status;
    String rejectedReason;
    LocalDateTime publishedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}