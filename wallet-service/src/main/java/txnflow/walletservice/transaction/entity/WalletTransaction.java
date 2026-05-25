package txnflow.walletservice.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import txnflow.walletservice.transaction.enums.TransactionCategory;
import txnflow.walletservice.transaction.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "wallet_transactions",
        indexes = {
                @Index(name = "idx_wallet_transaction_wallet_id", columnList = "wallet_id"),
                @Index(name = "idx_wallet_transaction_transfer_id", columnList = "transfer_id"),
                @Index(name = "idx_wallet_transaction_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wallet_id", nullable = false, updatable = false)
    private UUID walletId;

    @Column(name = "transfer_id", updatable = false)
    private UUID transferId;  // Nullable - only set for transfer transactions

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TransactionType type;  // DEBIT, CREDIT

    @Column(nullable = false, precision = 19, scale = 4, updatable = false)  // ✅ Changed to scale=4 for precision
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(length = 500, updatable = false)
    private String description;  // ✅ Optional but useful

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(updatable = false)
    private UUID counterpartyUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TransactionCategory category;

}