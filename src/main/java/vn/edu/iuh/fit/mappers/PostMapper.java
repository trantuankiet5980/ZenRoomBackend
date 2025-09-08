package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.PostDto;
import vn.edu.iuh.fit.entities.Post;

@Component
@RequiredArgsConstructor
public class PostMapper {

    private final UserMapper userMapper;
    private final PropertyMapper propertyMapper;

    public PostDto toDTO(Post post){
        if (post == null) return null;

        return PostDto.builder()
                .postId(post.getPostId())
                .landlord(post.getLandlord() != null ? userMapper.toDto(post.getLandlord()) : null)
                .property(post.getProperty() != null ? propertyMapper.toDto(post.getProperty()) : null)
                .title(post.getTitle())
                .description(post.getDescription())
                .isFireSafe(post.getIsFireSafe())
                .contactPhone(post.getContactPhone())
                .status(post.getStatus())
                .rejectedReason(post.getRejectedReason())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public Post toEntity(PostDto dto) {
        if (dto == null) return null;

        Post entity = new Post();
        entity.setPostId(dto.getPostId());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setIsFireSafe(dto.getIsFireSafe());
        entity.setContactPhone(dto.getContactPhone());
        entity.setStatus(dto.getStatus());
        entity.setRejectedReason(dto.getRejectedReason());
        entity.setPublishedAt(dto.getPublishedAt());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());

        // Landlord, Property sẽ được service set dựa trên id để tránh load quá sâu
        return entity;
    }
}
