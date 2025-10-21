package vn.edu.iuh.fit.dtos;

import java.time.LocalDate;

public record DailyPostCountDTO(LocalDate date, long totalPosts) {
}
