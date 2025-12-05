package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.dtos.requests.AdminWalletTransferRequest;
import vn.edu.iuh.fit.dtos.responses.WalletOverviewResponse;
import vn.edu.iuh.fit.services.WalletService;

@RestController
@RequestMapping("/api/v1/admin/wallet")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminWalletController {

    private final WalletService walletService;

    @PostMapping("/transfer")
    public ResponseEntity<WalletOverviewResponse> transferToUser(@RequestBody AdminWalletTransferRequest request) {
        return ResponseEntity.ok(walletService.adminTransferToUser(request));
    }
}
