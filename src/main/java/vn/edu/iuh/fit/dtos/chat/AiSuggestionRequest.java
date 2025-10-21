package vn.edu.iuh.fit.dtos.chat;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record AiSuggestionRequest(
        String userId,
        String city,
        String district,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate checkInDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate checkOutDate,
        List<String> furnishingPriorities,
        Integer limit
) {
    public int sanitizedLimit() {
        int desired = Objects.requireNonNullElse(limit, 3);
        if (desired < 2) {
            return 2;
        }
        return Math.min(desired, 3);
    }

    public List<String> safeFurnishingPriorities() {
        return furnishingPriorities == null ? List.of() : List.copyOf(furnishingPriorities);
    }
}
