package txnflow.walletservice.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import txnflow.walletservice.transaction.entity.WalletTransaction;

import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository
        extends JpaRepository<WalletTransaction, UUID> {

    Optional<WalletTransaction> findByReferenceId(String referenceId);
}