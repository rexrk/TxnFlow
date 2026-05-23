package txnflow.walletservice.wallet.service.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import txnflow.walletservice.constant.Currency;
import txnflow.walletservice.exception.WalletNotFoundException;
import txnflow.walletservice.security.CurrentUserProvider;
import txnflow.walletservice.wallet.dto.request.SetWalletPinRequest;
import txnflow.walletservice.wallet.dto.response.WalletResponse;
import txnflow.walletservice.wallet.entity.Wallet;
import txnflow.walletservice.wallet.enums.WalletStatus;
import txnflow.walletservice.wallet.mapper.WalletMapper;
import txnflow.walletservice.wallet.repository.WalletRepository;
import txnflow.walletservice.wallet.service.WalletService;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultWalletService implements WalletService {

    private final WalletRepository walletRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PasswordEncoder passwordEncoder;
    private final WalletMapper walletMapper;

    @Transactional
    @Override
    public WalletResponse createWalletForCurrentUser() {
        UUID userId = currentUserProvider.getCurrentAppUserId();
        return createWalletForUser(userId);
    }

    @Transactional
    @Override
    public WalletResponse createWalletForUser(UUID userId) {
        return walletRepository.findByUserId(userId)
                .map(walletMapper::toWalletResponse)
                .orElseGet(() -> {
                    Wallet wallet = Wallet.builder()
                            .userId(userId)
                            .balance(BigDecimal.ZERO)
                            .currency(Currency.INR)
                            .status(WalletStatus.ACTIVE)
                            .pinSet(false)
                            .build();

                    Wallet savedWallet = walletRepository.save(wallet);
                    log.info("Wallet created. walletId={} userId={}", savedWallet.getId(), userId);

                    return walletMapper.toWalletResponse(savedWallet);
                });
    }

    @Transactional(readOnly = true)
    @Override
    public WalletResponse getMyWallet() {
        UUID userId = currentUserProvider.getCurrentAppUserId();

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Wallet lookup failed. userId={}", userId);
                    return new WalletNotFoundException("Wallet not found");
                });

        return walletMapper.toWalletResponse(wallet);
    }

    @Transactional
    @Override
    public WalletResponse setWalletPin(SetWalletPinRequest request) {
        UUID userId = currentUserProvider.getCurrentAppUserId();

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Wallet PIN update failed: wallet not found. userId={}", userId);
                    return new WalletNotFoundException("Wallet not found");
                });

        wallet.setPinHash(passwordEncoder.encode(request.pin()));
        wallet.setPinSet(true);

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Wallet PIN updated. walletId={} userId={}", savedWallet.getId(), userId);

        return walletMapper.toWalletResponse(savedWallet);
    }

    @Transactional(readOnly = true)
    @Override
    public UUID getCurrentWalletId() {
        UUID userId = currentUserProvider.getCurrentAppUserId();

        return walletRepository.findWalletIdByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Current wallet lookup failed. userId={}", userId);
                    return new WalletNotFoundException("Wallet not found");
                });
    }
}
