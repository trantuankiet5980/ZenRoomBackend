package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.PostCreateDTO;
import vn.edu.iuh.fit.entities.Post;

@Component
public class PostMapper {
    public PostCreateDTO toDTO(Post post){
        return PostCreateDTO.builder()
                .landlordId(post.getLandlord().getUserId())
                .propertyId(post.getProperty().getPropertyId())
                .title(post.getTitle())
                .description(post.getDescription())
                .contactPhone(post.getContactPhone())
                .isFireSafe(post.getIsFireSafe())
                .status(post.getStatus().name())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .publishedAt(post.getPublishedAt())
                .build();
    }
}
