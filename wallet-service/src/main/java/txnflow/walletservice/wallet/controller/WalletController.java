package txnflow.walletservice.wallet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import txnflow.walletservice.wallet.dto.response.WalletResponse;
import txnflow.walletservice.wallet.service.internal.DefaultWalletService;

@RestController()
@RequiredArgsConstructor
public class WalletController {

    private final DefaultWalletService defaultWalletService;

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse createWallet() {
        return defaultWalletService.createWalletForCurrentUser();
    }
}