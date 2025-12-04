package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.WalletTransactionDto;
import vn.edu.iuh.fit.dtos.requests.WalletTransactionRequest;
import vn.edu.iuh.fit.dtos.responses.WalletOverviewResponse;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.Wallet;
import vn.edu.iuh.fit.entities.WalletTransaction;
import vn.edu.iuh.fit.entities.enums.WalletTransactionType;
import vn.edu.iuh.fit.mappers.WalletMapper;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.repositories.WalletRepository;
import vn.edu.iuh.fit.repositories.WalletTransactionRepository;
import vn.edu.iuh.fit.services.WalletService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final WalletMapper walletMapper;

    @Override
    public WalletOverviewResponse getCurrentWallet() {
        Wallet wallet = getOrCreateWalletForCurrentUser();
        return buildOverview(wallet);
    }

    @Override
    public WalletOverviewResponse deposit(WalletTransactionRequest request) {
        BigDecimal amount = requirePositiveAmount(request.getAmount());
        Wallet wallet = getOrCreateWalletForCurrentUser();

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        createTransaction(wallet, WalletTransactionType.MONEY_IN, amount, request.getDescription());

        return buildOverview(wallet);
    }

    @Override
    public WalletOverviewResponse withdraw(WalletTransactionRequest request) {
        BigDecimal amount = requirePositiveAmount(request.getAmount());
        Wallet wallet = getOrCreateWalletForCurrentUser();

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Số dư không đủ để thực hiện giao dịch này");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        createTransaction(wallet, WalletTransactionType.MONEY_OUT, amount, request.getDescription());

        return buildOverview(wallet);
    }

    private WalletOverviewResponse buildOverview(Wallet wallet) {
        List<WalletTransactionDto> transactions = walletTransactionRepository
                .findByWallet_WalletIdOrderByCreatedAtDesc(wallet.getWalletId())
                .stream()
                .map(walletMapper::toDto)
                .collect(Collectors.toList());

        return new WalletOverviewResponse(walletMapper.toDto(wallet), transactions);
    }

    private Wallet getOrCreateWalletForCurrentUser() {
        String userId = getCurrentUserId();
        return walletRepository.findByUser_UserId(userId)
                .orElseGet(() -> createNewWallet(userId));
    }

    private Wallet createNewWallet(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);

        return walletRepository.save(wallet);
    }

    private void createTransaction(Wallet wallet, WalletTransactionType type, BigDecimal amount, String description) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);

        walletTransactionRepository.save(transaction);
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private BigDecimal requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
        }
        return amount;
    }
}
