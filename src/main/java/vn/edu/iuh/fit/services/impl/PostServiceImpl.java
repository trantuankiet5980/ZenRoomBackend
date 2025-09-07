package vn.edu.iuh.fit.services.impl;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.PostCreateDTO;
import vn.edu.iuh.fit.entities.Post;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.repositories.PostRepository;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.PostService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public PostServiceImpl(PostRepository postRepository, PropertyRepository propertyRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    @Override
    public Post create(PostCreateDTO dto) {
        if(dto == null) throw new IllegalArgumentException("Request is null");
        if(isBlank(dto.getLandlordId())) throw new IllegalArgumentException("Landlord ID is required");
        if(isBlank(dto.getPropertyId())) throw new IllegalArgumentException("Property ID is required");
        if(isBlank(dto.getTitle())) throw new IllegalArgumentException("Post title is required");
        if(isBlank(dto.getContactPhone())) throw new IllegalArgumentException("Contact phone is required");

        User landlord = userRepository.findById(dto.getLandlordId())
                .orElseThrow(() -> new IllegalArgumentException("Landlord not found with ID: " + dto.getLandlordId()));
        Property property = propertyRepository.findById(dto.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found with ID: " + dto.getPropertyId()));

        Post p = new Post();
        p.setPostId(UUID.randomUUID().toString());
        p.setLandlord(landlord);
        p.setProperty(property);
        p.setTitle(dto.getTitle());
        p.setDescription(dto.getDescription());
        p.setContactPhone(dto.getContactPhone());
        p.setIsFireSafe(Boolean.TRUE.equals(dto.getIsFireSafe()));
        p.setStatus(PostStatus.PENDING); // mac dinh la PENDING cho duyet
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());

        return postRepository.save(p);
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
