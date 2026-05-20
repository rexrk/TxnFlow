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
                @UniqueConstraint(
                        name = "uk_wallet_transfer_sender_idempotency_key",
                        columnNames = {"sender_wallet_id", "idempotency_key"}
                )
        },
        indexes = {
                @Index(name = "idx_sender_wallet", columnList = "sender_wallet_id"),
                @Index(name = "idx_receiver_wallet", columnList = "receiver_wallet_id"),
                @Index(name = "idx_status_created", columnList = "status, created_at")
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

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, updatable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "sender_wallet_id", nullable = false, updatable = false)
    private UUID senderWalletId;

    @Column(name = "receiver_wallet_id", nullable = false, updatable = false)
    private UUID receiverWalletId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;  // PENDING, PROCESSING, COMPLETED, FAILED, REVERSED

    @Column(name = "debit_transaction_id")
    private UUID debitTransactionId;  // ✅ Link to sender's transaction

    @Column(name = "credit_transaction_id")
    private UUID creditTransactionId;  // ✅ Link to receiver's transaction

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(length = 1000)
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;  // ✅ When transfer actually completed

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}