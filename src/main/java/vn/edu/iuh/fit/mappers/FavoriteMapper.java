package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.FavoriteDto;
import vn.edu.iuh.fit.entities.Favorite;

@Component
@RequiredArgsConstructor
public class FavoriteMapper {

    private final UserMapper userMapper;
    private final PropertyMapper propertyMapper;

    public FavoriteDto toDto(Favorite entity) {
        if (entity == null) return null;

        return new FavoriteDto(
                entity.getFavoriteId(),
                entity.getTenant() != null ? userMapper.toDto(entity.getTenant()) : null,
                entity.getProperty() != null ? propertyMapper.toDto(entity.getProperty()) : null,
                entity.getCreatedAt()
        );
    }

    public Favorite toEntity(FavoriteDto dto) {
        if (dto == null) return null;

        Favorite entity = new Favorite();
        entity.setFavoriteId(dto.getFavoriteId());
        entity.setCreatedAt(dto.getCreatedAt());

        // tenant và property sẽ được set trong Service
        // bằng cách fetch từ DB theo id để tránh lazy loading/vòng lặp
        return entity;
    }
}
