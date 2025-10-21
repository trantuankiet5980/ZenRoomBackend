package vn.edu.iuh.fit.dtos.chat;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatPropertyDto(
        String propertyId,
        String title,
        BigDecimal price,
        Double area,
        Integer capacity,
        String district,
        String province,
        String address,
        String propertyType,
        String apartmentCategory,
        Integer bedrooms,
        Integer bathrooms,
        String thumbnailUrl,
        List<String> furnishings
) {
    public ChatPropertyDto {
        furnishings = furnishings == null ? List.of() : List.copyOf(furnishings);
    }
}
