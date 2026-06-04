package txnflow.walletservice.transaction.service.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import txnflow.walletservice.constant.Currency;
import txnflow.walletservice.exception.WalletTransactionNotFoundException;
import txnflow.walletservice.security.CurrentUserProvider;
import txnflow.walletservice.transaction.dto.response.WalletTransactionListItemResponse;
import txnflow.walletservice.transaction.dto.response.WalletTransactionResponse;
import txnflow.walletservice.transaction.entity.WalletTransaction;
import txnflow.walletservice.transaction.enums.TransactionCategory;
import txnflow.walletservice.transaction.enums.TransactionType;
import txnflow.walletservice.transaction.repository.WalletTransactionRepository;
import txnflow.walletservice.transaction.service.TransactionService;
import txnflow.walletservice.wallet.entity.Wallet;
import txnflow.walletservice.wallet.enums.WalletStatus;
import txnflow.walletservice.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class DefaultTransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private UUID user1;
    private UUID user2;
    private Wallet wallet1;
    private Wallet wallet2;

    @BeforeEach
    void setUp() {
        walletTransactionRepository.deleteAll();
        walletRepository.deleteAll();

        user1 = UUID.randomUUID();
        user2 = UUID.randomUUID();

        wallet1 = walletRepository.save(Wallet.builder()
                .userId(user1)
                .email("user1@email.com")
                .balance(BigDecimal.valueOf(1000))
                .currency(Currency.INR)
                .status(WalletStatus.ACTIVE)
                .pinSet(true)
                .pinHash("dummy")
                .build());

        wallet2 = walletRepository.save(Wallet.builder()
                .userId(user2)
                .email("user2@email.com")
                .balance(BigDecimal.valueOf(2000))
                .currency(Currency.INR)
                .status(WalletStatus.ACTIVE)
                .pinSet(true)
                .pinHash("dummy")
                .build());

        when(currentUserProvider.getCurrentAppUserId())
                .thenReturn(user1);
    }

    @Test
    void getTransactionShouldReturnTransactionForOwner() {
        WalletTransaction transaction = walletTransactionRepository.save(
                WalletTransaction.builder()
                        .walletId(wallet1.getId())
                        .transferId(UUID.randomUUID())
                        .type(TransactionType.DEBIT)
                        .category(TransactionCategory.PAID)
                        .amount(new BigDecimal("100.0000"))
                        .balanceAfter(new BigDecimal("900.0000"))
                        .currency(Currency.INR)
                        .description("test payment")
                        .counterpartyUserId(user2)
                        .build()
        );

        WalletTransactionResponse response =
                transactionService.getTransaction(transaction.getId());

        assertThat(response.id()).isEqualTo(transaction.getId());
        assertThat(response.category()).isEqualTo(TransactionCategory.PAID);
        assertThat(response.amount()).isEqualByComparingTo("100.0000");
    }

    @Test
    void getTransactionShouldNotReturnTransactionOfAnotherWallet() {
        WalletTransaction transaction = walletTransactionRepository.save(
                WalletTransaction.builder()
                        .walletId(wallet2.getId())
                        .transferId(UUID.randomUUID())
                        .type(TransactionType.CREDIT)
                        .category(TransactionCategory.RECEIVED)
                        .amount(new BigDecimal("100.0000"))
                        .balanceAfter(new BigDecimal("2100.0000"))
                        .currency(Currency.INR)
                        .description("other user transaction")
                        .counterpartyUserId(user1)
                        .build()
        );

        assertThatThrownBy(() ->
                transactionService.getTransaction(transaction.getId())
        ).isInstanceOf(WalletTransactionNotFoundException.class)
                .hasMessage("Transaction not found");
    }

    @Test
    void getRecentTransactionsShouldReturnLatest20Only() {
        for (int i = 1; i <= 25; i++) {
            walletTransactionRepository.save(
                    WalletTransaction.builder()
                            .walletId(wallet1.getId())
                            .transferId(UUID.randomUUID())
                            .type(TransactionType.DEBIT)
                            .category(TransactionCategory.PAID)
                            .amount(BigDecimal.valueOf(i))
                            .balanceAfter(BigDecimal.valueOf(1000 - i))
                            .currency(Currency.INR)
                            .description("txn " + i)
                            .counterpartyUserId(user2)
                            .build()
            );
        }

        List<WalletTransactionListItemResponse> response =
                transactionService.getRecentTransactions();

        assertThat(response).hasSize(20);
    }

    @Test
    void getMyTransactionsShouldReturnTransactionsWithinDateRange() {
        WalletTransaction transaction = walletTransactionRepository.save(
                WalletTransaction.builder()
                        .walletId(wallet1.getId())
                        .transferId(UUID.randomUUID())
                        .type(TransactionType.DEBIT)
                        .category(TransactionCategory.PAID)
                        .amount(new BigDecimal("100.0000"))
                        .balanceAfter(new BigDecimal("900.0000"))
                        .currency(Currency.INR)
                        .description("date range txn")
                        .counterpartyUserId(user2)
                        .build()
        );

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        List<WalletTransactionResponse> response =
                transactionService.getMyTransactions(today.minusDays(1), today.plusDays(1));

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(transaction.getId());
    }
}
