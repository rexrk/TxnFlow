package txnflow.walletservice.transfer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import txnflow.walletservice.transfer.enums.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "wallet_transfers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wallet_transfer_idempotency_key", columnNames = "idempotency_key")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sender_wallet_id", nullable = false, updatable = false)
    private UUID senderWalletId;

    @Column(name = "receiver_wallet_id", nullable = false, updatable = false)
    private UUID receiverWalletId;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false, length = 120)
    private String idempotencyKey;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}