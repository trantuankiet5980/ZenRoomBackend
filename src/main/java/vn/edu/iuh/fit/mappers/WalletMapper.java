package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.WalletDto;
import vn.edu.iuh.fit.dtos.WalletTransactionDto;
import vn.edu.iuh.fit.entities.Wallet;
import vn.edu.iuh.fit.entities.WalletTransaction;

@Component
public class WalletMapper {
    public WalletDto toDto(Wallet wallet) {
        if (wallet == null) return null;
        return new WalletDto(wallet.getWalletId(), wallet.getBalance(), wallet.getUpdatedAt());
    }

    public WalletTransactionDto toDto(WalletTransaction transaction) {
        if (transaction == null) return null;
        return new WalletTransactionDto(
                transaction.getTransactionId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
