package txnflow.walletservice.transfer.service.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import txnflow.walletservice.constant.Currency;
import txnflow.walletservice.exception.IdempotencyConflictException;
import txnflow.walletservice.exception.InvalidTransferException;
import txnflow.walletservice.exception.WalletNotFoundException;
import txnflow.walletservice.security.CurrentUserProvider;
import txnflow.walletservice.transaction.repository.WalletTransactionRepository;
import txnflow.walletservice.transfer.dto.request.TransferMoneyRequest;
import txnflow.walletservice.transfer.dto.response.TransferMoneyResponse;
import txnflow.walletservice.transfer.enums.TransferStatus;
import txnflow.walletservice.transfer.repository.WalletTransferRepository;
import txnflow.walletservice.transfer.service.TransferService;
import txnflow.walletservice.wallet.entity.Wallet;
import txnflow.walletservice.wallet.enums.WalletStatus;
import txnflow.walletservice.wallet.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class DefaultTransferServiceTest {

    @Autowired
    TransferService transferService;

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    WalletTransactionRepository walletTransactionRepository;

    @Autowired
    WalletTransferRepository walletTransferRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private List<UUID> userIds;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUp() {
        // Cleanup repos
        walletTransactionRepository.deleteAll();
        walletTransferRepository.deleteAll();
        walletRepository.deleteAll();

        // Initialize data
        userIds = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            walletRepository.save(Wallet.builder()
                    .userId(createUserId())
                    .balance(BigDecimal.valueOf(1000L * i))
                    .currency(Currency.INR)
                    .status(WalletStatus.ACTIVE)
                    .pinSet(true)
                    .pinHash(passwordEncoder.encode("1234"))
                    .build()
            );
        }

    }

    private UUID createUserId() {
        UUID userId = UUID.randomUUID();
        this.userIds.add(userId);
        return userId;
    }

    @Test
    void testSelfTransferShouldFail() {

        when(currentUserProvider.getCurrentAppUserId())
                .thenReturn(userIds.getFirst());

        TransferMoneyRequest request = new TransferMoneyRequest(
                userIds.getFirst(),
                new BigDecimal("100.0000"),
                "self-transfer-test",
                "1234",
                "self transfer"
        );

        assertThatThrownBy(() -> transferService.transferMoney(request))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Cannot transfer money to yourself");

        Wallet wallet = walletRepository
                .findByUserId(userIds.getFirst())
                .orElseThrow();

        assertThat(wallet.getBalance())
                .isEqualByComparingTo("1000.0000");

        assertThat(walletTransferRepository.findAll()).isEmpty();

        assertThat(walletTransactionRepository.findAll()).isEmpty();
    }

    @Test
    void testSenderWalletNotFoundShouldFail() {
        UUID missingSenderUserId = UUID.randomUUID();

        when(currentUserProvider.getCurrentAppUserId())
                .thenReturn(missingSenderUserId);

        TransferMoneyRequest request = new TransferMoneyRequest(
                userIds.get(1),
                new BigDecimal("100.0000"),
                "sender-wallet-missing-test",
                "1234",
                "sender missing"
        );

        assertThatThrownBy(() -> transferService.transferMoney(request))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessage("Sender wallet not found");

        assertThat(walletTransferRepository.findAll()).isEmpty();

        assertThat(walletTransactionRepository.findAll()).isEmpty();
    }

    @Test
    void testConcurrentTransfersWithSameIdempotencyKeyShouldCreateOnlyOneTransfer() throws Exception {

        when(currentUserProvider.getCurrentAppUserId())
                .thenReturn(userIds.getFirst());

        int requestCount = 5;
        BigDecimal amount = new BigDecimal("100.0000");
        String sameIdempotencyKey = "same-key-concurrent-test";

        try (ExecutorService executorService =
                     Executors.newFixedThreadPool(requestCount)) {

            CountDownLatch startLatch = new CountDownLatch(1);

            List<Callable<TransferMoneyResponse>> tasks = IntStream.range(0, requestCount)
                    .mapToObj(i -> (Callable<TransferMoneyResponse>) () -> {
                        startLatch.await();

                        TransferMoneyRequest request = new TransferMoneyRequest(
                                userIds.get(1),
                                amount,
                                sameIdempotencyKey,
                                "1234",
                                "same key concurrent test"
                        );

                        return transferService.transferMoney(request);
                    })
                    .toList();

            List<Future<TransferMoneyResponse>> futures = tasks.stream()
                    .map(executorService::submit)
                    .toList();

            startLatch.countDown();

            List<TransferMoneyResponse> responses = new ArrayList<>();

            for (Future<TransferMoneyResponse> future : futures) {
                responses.add(future.get());
            }

            UUID transferId = responses.getFirst().transferId();

            assertThat(responses)
                    .allMatch(response -> response.transferId().equals(transferId));
        }

        Wallet senderWallet = walletRepository
                .findByUserId(userIds.getFirst())
                .orElseThrow();

        Wallet receiverWallet = walletRepository
                .findByUserId(userIds.get(1))
                .orElseThrow();

        assertThat(senderWallet.getBalance()).isEqualByComparingTo("900.0000");
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("2100.0000");

        assertThat(walletTransferRepository.findAll()).hasSize(1);
        assertThat(walletTransactionRepository.findAll()).hasSize(2);
    }

    @Test
    void testSameIdempotencyKeyWithDifferentPayloadShouldThrowConflict() {
        when(currentUserProvider.getCurrentAppUserId())
                .thenReturn(userIds.getFirst());

        String sameKey = "same-key-different-payload-test";

        TransferMoneyRequest firstRequest = new TransferMoneyRequest(
                userIds.get(1),
                new BigDecimal("100.0000"),
                sameKey,
                "1234",
                "first request"
        );

        TransferMoneyRequest secondRequest = new TransferMoneyRequest(
                userIds.get(1),
                new BigDecimal("200.0000"),
                sameKey,
                "1234",
                "changed amount"
        );

        TransferMoneyResponse firstResponse = transferService.transferMoney(firstRequest);

        assertThat(firstResponse.status()).isEqualTo(TransferStatus.COMPLETED);

        assertThatThrownBy(() -> transferService.transferMoney(secondRequest))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessage("Idempotency key already used with different request");

        Wallet senderWallet = walletRepository
                .findByUserId(userIds.getFirst())
                .orElseThrow();

        Wallet receiverWallet = walletRepository
                .findByUserId(userIds.get(1))
                .orElseThrow();

        assertThat(senderWallet.getBalance()).isEqualByComparingTo("900.0000");
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("2100.0000");

        assertThat(walletTransferRepository.findAll()).hasSize(1);
        assertThat(walletTransactionRepository.findAll()).hasSize(2);
    }

    @Test
    void testSameIdempotencyKeyWithSamePayloadShouldReturnExistingTransfer() {
        when(currentUserProvider.getCurrentAppUserId())
                .thenReturn(userIds.getFirst());

        String sameKey = "same-key-same-payload-test";

        TransferMoneyRequest request = new TransferMoneyRequest(
                userIds.get(1),
                new BigDecimal("100.0000"),
                sameKey,
                "1234",
                "same request"
        );

        TransferMoneyResponse firstResponse = transferService.transferMoney(request);

        TransferMoneyResponse secondResponse = transferService.transferMoney(request);

        assertThat(secondResponse.transferId()).isEqualTo(firstResponse.transferId());

        assertThat(secondResponse.idempotentReplay()).isTrue();

        Wallet senderWallet = walletRepository
                .findByUserId(userIds.getFirst())
                .orElseThrow();

        Wallet receiverWallet = walletRepository
                .findByUserId(userIds.get(1))
                .orElseThrow();

        assertThat(senderWallet.getBalance()).isEqualByComparingTo("900.0000");
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("2100.0000");

        assertThat(walletTransferRepository.findAll()).hasSize(1);
        assertThat(walletTransactionRepository.findAll()).hasSize(2);
    }

}