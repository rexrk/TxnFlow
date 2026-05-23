package txnflow.walletservice.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import txnflow.walletservice.transaction.entity.WalletTransaction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WalletTransactionRepository
        extends JpaRepository<WalletTransaction, UUID> {

    List<WalletTransaction> findTop20ByWalletIdOrderByCreatedAtDesc(
            UUID walletId
    );

    List<WalletTransaction> findByWalletIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID walletId,
            Instant from,
            Instant to
    );
}