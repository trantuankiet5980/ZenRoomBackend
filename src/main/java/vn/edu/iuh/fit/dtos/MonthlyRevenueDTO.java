package vn.edu.iuh.fit.dtos;

import java.math.BigDecimal;

public record MonthlyRevenueDTO(int year, int month, BigDecimal revenue) {
}
