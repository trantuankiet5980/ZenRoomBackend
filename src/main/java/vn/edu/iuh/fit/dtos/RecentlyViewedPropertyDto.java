package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;

@Value
public class RecentlyViewedPropertyDto implements Serializable {
    PropertyDto property;
    LocalDateTime lastViewedAt;
}
