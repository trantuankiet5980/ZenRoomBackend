package vn.edu.iuh.fit.dtos;

import lombok.Value;

@Value
public class FurnishingWithQuantityDto {
    String furnishingId;
    String furnishingName;
    Integer quantity;
}
