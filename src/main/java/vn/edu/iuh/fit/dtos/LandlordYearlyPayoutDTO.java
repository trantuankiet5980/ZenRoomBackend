package vn.edu.iuh.fit.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LandlordYearlyPayoutDTO {
    private String landlordId;
    private String landlordName;
    private String landlordPhone;
    private List<BigDecimal> monthlyPayouts;
    private BigDecimal total;
}
