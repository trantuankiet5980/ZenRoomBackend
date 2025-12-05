package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.WalletTransaction;

import java.time.LocalDateTime;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, String> {
    List<WalletTransaction> findByWallet_WalletIdOrderByCreatedAtDesc(String walletId);

    List<WalletTransaction> findByWallet_WalletIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String walletId,
            LocalDateTime start,
            LocalDateTime end
    );
}
