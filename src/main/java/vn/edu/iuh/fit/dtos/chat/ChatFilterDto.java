package vn.edu.iuh.fit.dtos.chat;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatFilterDto(
        String provinceCode,
        String provinceName,
        String districtCode,
        String districtName,
        Long priceMin,
        Long priceMax,
        Double areaMin,
        Double areaMax,
        Integer capacityMin,
        Integer bedroomsMin,
        Integer bathroomsMin,
        String propertyType,
        String apartmentCategory,
        List<String> keywords
) {
}
