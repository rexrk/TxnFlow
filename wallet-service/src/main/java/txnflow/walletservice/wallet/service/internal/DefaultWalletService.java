package txnflow.walletservice.wallet.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import txnflow.walletservice.security.CurrentUserProvider;
import txnflow.walletservice.wallet.dto.response.WalletResponse;
import txnflow.walletservice.wallet.entity.Wallet;
import txnflow.walletservice.wallet.enums.WalletStatus;
import txnflow.walletservice.wallet.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultWalletService {

    private final WalletRepository walletRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public WalletResponse createWalletForCurrentUser() {
        UUID userId = currentUserProvider.getCurrentAppUserId();
        return createWalletForUser(userId);
    }

    @Transactional
    public WalletResponse createWalletForUser(UUID userId) {
        return walletRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    Wallet wallet = Wallet.builder()
                            .userId(userId)
                            .balance(BigDecimal.ZERO)
                            .currency("INR")
                            .status(WalletStatus.ACTIVE)
                            .build();

                    return toResponse(walletRepository.save(wallet));
                });
    }

    private WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus()
        );
    }
}