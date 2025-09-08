package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.PropertyMediaDto;
import vn.edu.iuh.fit.entities.PropertyMedia;

@Component
public class PropertyMediaMapper {

    public PropertyMediaDto toDto(PropertyMedia e) {
        if (e == null) return null;
        return new PropertyMediaDto(
                e.getMediaId(),
                (e.getProperty() != null ? e.getProperty().getPropertyId() : null),
                e.getMediaType(),
                e.getUrl(),
                e.getPosterUrl(),
                e.getSortOrder(),
                e.getIsCover()
        );
    }

    public PropertyMedia toEntity(PropertyMediaDto d) {
        if (d == null) return null;
        return PropertyMedia.builder()
                .mediaId(d.getMediaId())
                .mediaType(d.getMediaType())
                .url(d.getUrl())
                .posterUrl(d.getPosterUrl())
                .sortOrder(d.getSortOrder())
                .isCover(d.getIsCover())
                .build();
    }
}
