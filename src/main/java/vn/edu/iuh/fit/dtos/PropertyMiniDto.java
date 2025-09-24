package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class PropertyMiniDto {
    String propertyId;
    String title;
    BigDecimal price;
    String address;
    String thumbnailUrl;
}
