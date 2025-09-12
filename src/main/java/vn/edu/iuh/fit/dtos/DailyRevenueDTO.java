package vn.edu.iuh.fit.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueDTO(LocalDate date, BigDecimal revenue) {
}
