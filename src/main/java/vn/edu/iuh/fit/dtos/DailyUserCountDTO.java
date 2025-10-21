package vn.edu.iuh.fit.dtos;

import java.time.LocalDate;

public record DailyUserCountDTO(LocalDate date, long totalUsers) {
}
