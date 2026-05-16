package txnflow.walletservice.wallet.service;

import txnflow.walletservice.wallet.dto.response.WalletResponse;

import java.util.UUID;

public interface WalletService {

    WalletResponse createWalletForCurrentUser();

    WalletResponse createWalletForUser(UUID userId);
}