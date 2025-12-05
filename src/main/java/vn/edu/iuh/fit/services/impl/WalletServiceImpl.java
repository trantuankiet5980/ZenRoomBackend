package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.WalletTransactionDto;
import vn.edu.iuh.fit.dtos.requests.AdminWalletTransferRequest;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
    public WalletOverviewResponse getCurrentWallet(Integer month, Integer year) {
        Wallet wallet = getOrCreateWalletForCurrentUser();
        DateRange dateRange = resolveDateRange(month, year);
        return buildOverview(wallet, dateRange);
    }

    @Override
    public WalletOverviewResponse deposit(WalletTransactionRequest request) {
        BigDecimal amount = requirePositiveAmount(request.getAmount());
        Wallet wallet = getOrCreateWalletForCurrentUser();

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        createTransaction(wallet, WalletTransactionType.MONEY_IN, amount, request.getDescription());

        return buildOverview(wallet, DateRange.none());
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

        return buildOverview(wallet, DateRange.none());
    }

    @Override
    public WalletOverviewResponse adminTransferToUser(AdminWalletTransferRequest request) {
        BigDecimal amount = requirePositiveAmount(request.getAmount());
        Wallet wallet = getOrCreateWalletForUser(request.getUserId());

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        String description = request.getDescription() != null
                ? request.getDescription()
                : "Thanh toán doanh thu";

        createTransaction(wallet, WalletTransactionType.MONEY_IN, amount, description);

        return buildOverview(wallet, DateRange.none());
    }

    private WalletOverviewResponse buildOverview(Wallet wallet, DateRange dateRange) {
        List<WalletTransaction> transactionEntities = dateRange.isFiltered()
                ? walletTransactionRepository.findByWallet_WalletIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                wallet.getWalletId(), dateRange.start(), dateRange.end())
                : walletTransactionRepository.findByWallet_WalletIdOrderByCreatedAtDesc(wallet.getWalletId());

        List<WalletTransactionDto> transactions = transactionEntities.stream()
                .map(walletMapper::toDto)
                .collect(Collectors.toList());

        return new WalletOverviewResponse(walletMapper.toDto(wallet), transactions);
    }

    private Wallet getOrCreateWalletForCurrentUser() {
        String userId = getCurrentUserId();
        return walletRepository.findByUser_UserId(userId)
                .orElseGet(() -> createNewWallet(userId));
    }

    private Wallet getOrCreateWalletForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("UserId không hợp lệ");
        }

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

    private DateRange resolveDateRange(Integer month, Integer year) {
        if (year == null && month == null) {
            return DateRange.none();
        }

        if (month != null && (month < 1 || month > 12)) {
            throw new IllegalArgumentException("Tháng không hợp lệ. Tháng phải từ 1 đến 12");
        }

        if (year == null) {
            throw new IllegalArgumentException("Vui lòng cung cấp năm khi lọc theo tháng");
        }

        if (month == null) {
            LocalDate startOfYear = LocalDate.of(year, 1, 1);
            LocalDate startOfNextYear = startOfYear.plusYears(1);
            return new DateRange(startOfYear.atStartOfDay(), startOfNextYear.atStartOfDay());
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        return new DateRange(start, end);
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) {
        static DateRange none() {
            return new DateRange(null, null);
        }

        boolean isFiltered() {
            return start != null && end != null;
        }
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
