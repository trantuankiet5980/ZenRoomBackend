package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.SearchHistory}
 */
@Value
public class SearchHistoryDto implements Serializable {
    String searchId;
    UserDto tenant;
    String keyword;
    String filters;
    LocalDateTime createdAt;
}