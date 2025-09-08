package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.PostDto;
import vn.edu.iuh.fit.entities.Post;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.mappers.PostMapper;
import vn.edu.iuh.fit.repositories.PostRepository;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.PostService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;

    @Transactional
    @Override
    public Post create(PostDto dto) {

        Post entity = postMapper.toEntity(dto);
        // Gắn landlord
        if (dto.getLandlord() != null && dto.getLandlord().getUserId() != null) {
            entity.setLandlord(userRepository.findById(dto.getLandlord().getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Landlord not found")));
        }
        // Gắn property
        if (dto.getProperty() != null && dto.getProperty().getPropertyId() != null) {
            entity.setProperty(propertyRepository.findById(dto.getProperty().getPropertyId())
                    .orElseThrow(() -> new EntityNotFoundException("Property not found")));
        }
        entity.setPostId(UUID.randomUUID().toString());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setContactPhone(dto.getContactPhone());
        entity.setIsFireSafe(Boolean.TRUE.equals(dto.getIsFireSafe()));
        entity.setStatus(PostStatus.PENDING); // mac dinh la PENDING cho duyet
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        return postRepository.save(entity);
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
