package txnflow.walletservice.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import txnflow.walletservice.transfer.entity.WalletTransfer;

import java.util.Optional;
import java.util.UUID;

public interface WalletTransferRepository extends JpaRepository<WalletTransfer, UUID> {

    Optional<WalletTransfer> findBySenderWalletIdAndIdempotencyKey(
            UUID senderWalletId,
            String idempotencyKey
    );
}