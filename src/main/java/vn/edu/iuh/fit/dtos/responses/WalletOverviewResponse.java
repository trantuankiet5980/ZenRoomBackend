package vn.edu.iuh.fit.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.dtos.WalletDto;
import vn.edu.iuh.fit.dtos.WalletTransactionDto;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletOverviewResponse {
    private WalletDto wallet;
    private List<WalletTransactionDto> transactions;
}
