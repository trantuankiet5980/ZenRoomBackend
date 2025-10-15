package vn.edu.iuh.fit.dtos.chat;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

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
        String thumbnailUrl
) {
}
