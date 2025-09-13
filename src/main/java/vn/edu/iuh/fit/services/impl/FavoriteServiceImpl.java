package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.FavoriteDto;
import vn.edu.iuh.fit.dtos.requests.FavoriteRequest;
import vn.edu.iuh.fit.entities.Favorite;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.mappers.FavoriteMapper;
import vn.edu.iuh.fit.repositories.FavoriteRepository;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.FavoriteService;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final FavoriteMapper favoriteMapper;

    private String getTenantIdFromAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @Override
    public FavoriteDto addFavorite(FavoriteRequest request) {
        String tenantId = getTenantIdFromAuth();

        if (favoriteRepository.existsByTenant_UserIdAndProperty_PropertyId(tenantId, request.getPropertyId())) {
            throw new RuntimeException("Phòng này đã có trong danh sách yêu thích");
        }

        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant không tồn tại"));
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Property không tồn tại"));

        Favorite favorite = new Favorite();
        favorite.setTenant(tenant);
        favorite.setProperty(property);

        favoriteRepository.save(favorite);
        return favoriteMapper.toDto(favorite);
    }

    @Override
    public List<FavoriteDto> getFavorites() {
        String tenantId = getTenantIdFromAuth();
        return favoriteRepository.findByTenant_UserId(tenantId)
                .stream()
                .map(favoriteMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void removeFavorite(String propertyId) {
        String tenantId = getTenantIdFromAuth();
        favoriteRepository.deleteByTenant_UserIdAndProperty_PropertyId(tenantId, propertyId);
    }
}

