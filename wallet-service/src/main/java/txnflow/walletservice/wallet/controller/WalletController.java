package txnflow.walletservice.wallet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import txnflow.walletservice.wallet.dto.response.WalletResponse;
import txnflow.walletservice.wallet.service.WalletService;

@RestController()
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse createWallet() {
        return walletService.createWalletForCurrentUser();
    }

    @GetMapping("/me")
    public WalletResponse getMyWallet() {
        return walletService.getMyWallet();
    }
}