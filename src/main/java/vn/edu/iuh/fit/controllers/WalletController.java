package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.requests.WalletTransactionRequest;
import vn.edu.iuh.fit.dtos.responses.WalletOverviewResponse;
import vn.edu.iuh.fit.services.WalletService;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<WalletOverviewResponse> getWallet(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(walletService.getCurrentWallet(month, year));
    }

    @PostMapping("/deposit")
    public ResponseEntity<WalletOverviewResponse> deposit(@RequestBody WalletTransactionRequest request) {
        return ResponseEntity.ok(walletService.deposit(request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WalletOverviewResponse> withdraw(@RequestBody WalletTransactionRequest request) {
        return ResponseEntity.ok(walletService.withdraw(request));
    }
}
