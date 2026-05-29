package txnflow.walletservice.orchestration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import txnflow.walletservice.constant.Currency;
import txnflow.walletservice.exception.WalletNotFoundException;
import txnflow.walletservice.kafka.constant.KafkaTopic;
import txnflow.walletservice.transaction.entity.WalletTransaction;
import txnflow.walletservice.transaction.enums.TransactionCategory;
import txnflow.walletservice.transaction.enums.TransactionType;
import txnflow.walletservice.transaction.repository.WalletTransactionRepository;
import txnflow.walletservice.kafka.event.WalletCreditEvent;
import txnflow.walletservice.wallet.entity.Wallet;
import txnflow.walletservice.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopupProcessor {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional
    @KafkaListener(topics = KafkaTopic.CREDIT_WALLET_ON_TOPUP)
    public void processTopup(WalletCreditEvent event) {

        UUID userId = event.userId();
        UUID ledgerId = event.ledgerId();
        BigDecimal amount = BigDecimal.valueOf(event.amount());

        log.info(
                "Processing topup. userId={} amount={} ledgerId={}",
                userId,
                amount,
                ledgerId
        );

        // 1. Idempotency guard (application level)
        boolean alreadyProcessed = walletTransactionRepository.existsByLedgerId(ledgerId);

        if (alreadyProcessed) {
            log.info("Duplicate topup ignored. ledgerId={}", ledgerId);
            return;
        }

        // 2. Fetch wallet with pessimistic lock
        Wallet userWallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> {
                    log.warn("Topup failed: wallet not found. userId={}", userId);
                    return new WalletNotFoundException("Wallet not found");
                });

        // 3. Calculate updated balance
        BigDecimal balanceAfter = userWallet.getBalance().add(amount);

        // 4. Update wallet balance
        userWallet.setBalance(balanceAfter);

        // 5. Create immutable wallet transaction
        try {
            walletTransactionRepository.save(
                    WalletTransaction.builder()
                            .walletId(userWallet.getId())
                            .ledgerId(ledgerId)
                            .type(TransactionType.CREDIT)
                            .amount(amount)
                            .balanceAfter(balanceAfter)
                            .currency(Currency.INR)
                            .description("Top-up via Razorpay")
                            .category(TransactionCategory.TOP_UP)
                            .build()
            );

            log.info(
                    "Topup completed successfully. userId={} ledgerId={} amount={} balanceAfter={}",
                    userId,
                    ledgerId,
                    amount,
                    balanceAfter
            );

        } catch (DataIntegrityViolationException ex) {
            log.warn(
                    "Duplicate topup detected at DB layer. ledgerId={}",
                    ledgerId
            );

        }
    }
}