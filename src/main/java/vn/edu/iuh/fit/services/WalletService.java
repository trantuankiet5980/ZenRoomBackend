package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.requests.WalletTransactionRequest;
import vn.edu.iuh.fit.dtos.responses.WalletOverviewResponse;

public interface WalletService {
    WalletOverviewResponse getCurrentWallet();

    WalletOverviewResponse deposit(WalletTransactionRequest request);

    WalletOverviewResponse withdraw(WalletTransactionRequest request);
}
