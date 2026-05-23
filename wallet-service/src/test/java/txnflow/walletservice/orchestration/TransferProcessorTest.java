package txnflow.walletservice.orchestration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import txnflow.walletservice.constant.Currency;
import txnflow.walletservice.exception.InsufficientBalanceException;
import txnflow.walletservice.exception.InvalidTransferException;
import txnflow.walletservice.exception.WalletNotFoundException;
import txnflow.walletservice.transaction.entity.WalletTransaction;
import txnflow.walletservice.transaction.enums.TransactionType;
import txnflow.walletservice.transaction.repository.WalletTransactionRepository;
import txnflow.walletservice.transfer.dto.request.TransferMoneyRequest;
import txnflow.walletservice.transfer.dto.response.TransferMoneyResponse;
import txnflow.walletservice.transfer.entity.WalletTransfer;
import txnflow.walletservice.transfer.enums.TransferStatus;
import txnflow.walletservice.transfer.repository.WalletTransferRepository;
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
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TransferProcessorTest {

    @Autowired
    TransferProcessor transferProcessor;

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    WalletTransactionRepository walletTransactionRepository;

    @Autowired
    WalletTransferRepository walletTransferRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private List<UUID> userIds;

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
    void testProcessTransferSuccess() {
        TransferMoneyRequest request = new TransferMoneyRequest(
                userIds.get(1),
                new BigDecimal("100.0000"),
                "test-key-001",
                "1234",
                "success transfer test"
        );

        TransferMoneyResponse response = transferProcessor.processTransfer(
            userIds.getFirst(),
                request,
                "request-fingerprint-dummy-01"
        );

        assertNotNull(response);
        assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("100.0000"));


        Wallet senderWallet = walletRepository.findByUserId(userIds.getFirst()).orElseThrow();
        Wallet receiverWallet = walletRepository.findByUserId(userIds.get(1)).orElseThrow();

        assertThat(senderWallet.getBalance()).isEqualByComparingTo("900.0000");
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("2100.0000");

        List<WalletTransaction> transactions = walletTransactionRepository.findAll();

        assertThat(transactions).hasSize(2);

        assertThat(transactions)
                .extracting(WalletTransaction::getType)
                .containsExactlyInAnyOrder(TransactionType.DEBIT, TransactionType.CREDIT);

        WalletTransfer transfer = walletTransferRepository.findAll().getFirst();

        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(transfer.getDebitTransactionId()).isNotNull();
        assertThat(transfer.getCreditTransactionId()).isNotNull();

    }

    @Test
    void testProcessTransferFailsWhenInsufficientBalance() {
        TransferMoneyRequest request = new TransferMoneyRequest(
                userIds.get(1),
                new BigDecimal("5000.0000"),
                "test-key-insufficient-001",
                "1234",
                "insufficient balance test"
        );

        assertThatThrownBy(() ->
                transferProcessor.processTransfer(
                        userIds.getFirst(),
                        request,
                        "request-fingerprint-dummy-02"
                )
        ).isInstanceOf(InsufficientBalanceException.class)
                .hasMessageStartingWith("Insufficient wallet balance");

        Wallet senderWallet = walletRepository.findByUserId(userIds.getFirst()).orElseThrow();
        Wallet receiverWallet = walletRepository.findByUserId(userIds.get(1)).orElseThrow();

        assertThat(senderWallet.getBalance()).isEqualByComparingTo("1000.0000");
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("2000.0000");

        assertThat(walletTransferRepository.findAll()).isEmpty();
        assertThat(walletTransactionRepository.findAll()).isEmpty();
    }

    @Test
    void testProcessTransferFailsWhenWalletPinIsInvalid() {
        TransferMoneyRequest request = new TransferMoneyRequest(
                userIds.get(1),
                new BigDecimal("100.0000"),
                "test-key-invalid-pin-001",
                "9999",
                "invalid pin test"
        );

        assertThatThrownBy(() ->
                transferProcessor.processTransfer(
                        userIds.getFirst(),
                        request,
                        "request-fingerprint-dummy-03"
                )
        ).isInstanceOf(InvalidTransferException.class)
                .hasMessage("Invalid wallet PIN");

        Wallet senderWallet = walletRepository.findByUserId(userIds.getFirst()).orElseThrow();
        Wallet receiverWallet = walletRepository.findByUserId(userIds.get(1)).orElseThrow();

        assertThat(senderWallet.getBalance()).isEqualByComparingTo("1000.0000");
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("2000.0000");

        assertThat(walletTransferRepository.findAll()).isEmpty();
        assertThat(walletTransactionRepository.findAll()).isEmpty();
    }

    @Test
    void testProcessTransferFailsWhenReceiverWalletDoesNotExist() {

        UUID missingReceiverId = UUID.randomUUID();

        TransferMoneyRequest request = new TransferMoneyRequest(
                missingReceiverId,
                new BigDecimal("100.0000"),
                "test-key-missing-receiver-001",
                "1234",
                "missing receiver test"
        );

        assertThatThrownBy(() ->
                transferProcessor.processTransfer(
                        userIds.getFirst(),
                        request,
                        "request-fingerprint-dummy-04"
                )
        ).isInstanceOf(WalletNotFoundException.class)
                .hasMessage("Receiver wallet not found");

        Wallet senderWallet = walletRepository
                .findByUserId(userIds.getFirst())
                .orElseThrow();

        assertThat(senderWallet.getBalance())
                .isEqualByComparingTo("1000.0000");

        assertThat(walletTransferRepository.findAll()).isEmpty();

        assertThat(walletTransactionRepository.findAll()).isEmpty();
    }

    @Test
    void testProcessTransferWithSameIdempotencyKeyShouldNotCreateDuplicateTransfer() {

        TransferMoneyRequest request = new TransferMoneyRequest(
                userIds.get(1),
                new BigDecimal("100.0000"),
                "duplicate-key-001",
                "1234",
                "duplicate transfer test"
        );

        TransferMoneyResponse firstResponse =
                transferProcessor.processTransfer(
                        userIds.getFirst(),
                        request,
                        "request-fingerprint-dummy-05"
                );

        assertThat(firstResponse).isNotNull();

        assertThatThrownBy(() ->
                transferProcessor.processTransfer(
                        userIds.getFirst(),
                        request,
                        "request-fingerprint-dummy-05"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);

        Wallet senderWallet = walletRepository
                .findByUserId(userIds.getFirst())
                .orElseThrow();

        Wallet receiverWallet = walletRepository
                .findByUserId(userIds.get(1))
                .orElseThrow();

        assertThat(senderWallet.getBalance())
                .isEqualByComparingTo("900.0000");

        assertThat(receiverWallet.getBalance())
                .isEqualByComparingTo("2100.0000");

        assertThat(walletTransferRepository.findAll())
                .hasSize(1);

        assertThat(walletTransactionRepository.findAll())
                .hasSize(2);
    }

    @Test
    void testProcessTransferFailsWhenSenderWalletIsNotActive() {
        Wallet senderWallet = walletRepository
                .findByUserId(userIds.getFirst())
                .orElseThrow();

        senderWallet.setStatus(WalletStatus.FROZEN);
        walletRepository.save(senderWallet);

        TransferMoneyRequest request = new TransferMoneyRequest(
                userIds.get(1),
                new BigDecimal("100.0000"),
                "test-key-sender-frozen-001",
                "1234",
                "sender frozen test"
        );

        assertThatThrownBy(() ->
                transferProcessor.processTransfer(
                        userIds.getFirst(),
                        request,
                        "request-fingerprint-dummy-06"
                )
        ).isInstanceOf(InvalidTransferException.class)
                .hasMessage("Sender wallet is not active");

        Wallet receiverWallet = walletRepository
                .findByUserId(userIds.get(1))
                .orElseThrow();

        assertThat(senderWallet.getBalance()).isEqualByComparingTo("1000.0000");
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("2000.0000");
        assertThat(walletTransferRepository.findAll()).isEmpty();
        assertThat(walletTransactionRepository.findAll()).isEmpty();
    }

    @Test
    void testProcessTransferFailsWhenReceiverWalletIsNotActive() {

        Wallet receiverWallet = walletRepository
                .findByUserId(userIds.get(1))
                .orElseThrow();

        receiverWallet.setStatus(WalletStatus.FROZEN);

        walletRepository.save(receiverWallet);

        TransferMoneyRequest request = new TransferMoneyRequest(
                userIds.get(1),
                new BigDecimal("100.0000"),
                "test-key-receiver-frozen-001",
                "1234",
                "receiver frozen test"
        );

        assertThatThrownBy(() ->
                transferProcessor.processTransfer(
                        userIds.getFirst(),
                        request,
                        "request-fingerprint-dummy-07"
                )
        ).isInstanceOf(InvalidTransferException.class)
                .hasMessage("Receiver wallet is not active");

        Wallet senderWallet = walletRepository
                .findByUserId(userIds.getFirst())
                .orElseThrow();

        receiverWallet = walletRepository
                .findByUserId(userIds.get(1))
                .orElseThrow();

        assertThat(senderWallet.getBalance())
                .isEqualByComparingTo("1000.0000");

        assertThat(receiverWallet.getBalance())
                .isEqualByComparingTo("2000.0000");

        assertThat(walletTransferRepository.findAll()).isEmpty();

        assertThat(walletTransactionRepository.findAll()).isEmpty();
    }

    @Test
    void testProcessTransferFailsWhenWalletPinIsNotSet() {

        Wallet senderWallet = walletRepository
                .findByUserId(userIds.getFirst())
                .orElseThrow();

        senderWallet.setPinSet(false);
        senderWallet.setPinHash(null);

        walletRepository.save(senderWallet);

        TransferMoneyRequest request = new TransferMoneyRequest(
                userIds.get(1),
                new BigDecimal("100.0000"),
                "test-key-pin-not-set-001",
                "1234",
                "pin not set test"
        );

        assertThatThrownBy(() ->
                transferProcessor.processTransfer(
                        userIds.getFirst(),
                        request,
                        "request-fingerprint-dummy-08"
                )
        ).isInstanceOf(InvalidTransferException.class)
                .hasMessage("Wallet PIN is not set");

        Wallet receiverWallet = walletRepository
                .findByUserId(userIds.get(1))
                .orElseThrow();

        senderWallet = walletRepository
                .findByUserId(userIds.getFirst())
                .orElseThrow();

        assertThat(senderWallet.getBalance())
                .isEqualByComparingTo("1000.0000");

        assertThat(receiverWallet.getBalance())
                .isEqualByComparingTo("2000.0000");

        assertThat(walletTransferRepository.findAll()).isEmpty();

        assertThat(walletTransactionRepository.findAll()).isEmpty();
    }

    @Test
    void testConcurrentTransfersFromSameSenderShouldNotCorruptBalance() throws Exception {

        int transferCount = 5;
        BigDecimal amount = new BigDecimal("300.0000");

        try (ExecutorService executorService =
                     Executors.newFixedThreadPool(transferCount)) {

            CountDownLatch startLatch = new CountDownLatch(1);

            List<Callable<Boolean>> tasks = IntStream.range(0, transferCount)
                    .mapToObj(i -> (Callable<Boolean>) () -> {
                        startLatch.await();

                        TransferMoneyRequest request = new TransferMoneyRequest(
                                userIds.get(1),
                                amount,
                                "concurrent-same-sender-" + i,
                                "1234",
                                "concurrent same sender test"
                        );

                        try {
                            transferProcessor.processTransfer(
                                    userIds.getFirst(),
                                    request,
                                    "fingerprint-concurrent-" + i
                            );
                            return true;

                        } catch (InsufficientBalanceException ex) {
                            return false;
                        }
                    })
                    .toList();

            List<Future<Boolean>> futures = tasks.stream()
                    .map(executorService::submit)
                    .toList();

            startLatch.countDown();

            int successCount = 0;
            int failureCount = 0;

            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }

            assertThat(successCount).isEqualTo(3);
            assertThat(failureCount).isEqualTo(2);
        }

        Wallet senderWallet = walletRepository
                .findByUserId(userIds.getFirst())
                .orElseThrow();

        Wallet receiverWallet = walletRepository
                .findByUserId(userIds.get(1))
                .orElseThrow();

        assertThat(senderWallet.getBalance())
                .isEqualByComparingTo("100.0000");

        assertThat(receiverWallet.getBalance())
                .isEqualByComparingTo("2900.0000");

        assertThat(walletTransferRepository.findAll())
                .hasSize(3);

        assertThat(walletTransactionRepository.findAll())
                .hasSize(6);
    }

    @Test
    void testConcurrentTransfersToSameReceiverShouldNotCorruptBalance() throws Exception {

        UUID receiverUserId = userIds.get(2);
        BigDecimal amount = new BigDecimal("100.0000");

        List<UUID> senderIds = List.of(
                userIds.get(0),
                userIds.get(1)
        );

        int transferCount = senderIds.size();

        try (ExecutorService executorService =
                     Executors.newFixedThreadPool(transferCount)) {

            CountDownLatch startLatch = new CountDownLatch(1);

            List<Callable<Boolean>> tasks = IntStream.range(0, transferCount)
                    .mapToObj(i -> (Callable<Boolean>) () -> {
                        startLatch.await();

                        TransferMoneyRequest request = new TransferMoneyRequest(
                                receiverUserId,
                                amount,
                                "concurrent-same-receiver-" + i,
                                "1234",
                                "concurrent same receiver test"
                        );

                        transferProcessor.processTransfer(
                                senderIds.get(i),
                                request,
                                "fingerprint-same-receiver-" + i
                        );

                        return true;
                    })
                    .toList();

            List<Future<Boolean>> futures = tasks.stream()
                    .map(executorService::submit)
                    .toList();

            startLatch.countDown();

            for (Future<Boolean> future : futures) {
                assertThat(future.get()).isTrue();
            }
        }

        Wallet senderOne = walletRepository.findByUserId(userIds.get(0)).orElseThrow();
        Wallet senderTwo = walletRepository.findByUserId(userIds.get(1)).orElseThrow();
        Wallet receiver = walletRepository.findByUserId(receiverUserId).orElseThrow();

        assertThat(senderOne.getBalance()).isEqualByComparingTo("900.0000");
        assertThat(senderTwo.getBalance()).isEqualByComparingTo("1900.0000");
        assertThat(receiver.getBalance()).isEqualByComparingTo("3200.0000");

        assertThat(walletTransferRepository.findAll()).hasSize(2);
        assertThat(walletTransactionRepository.findAll()).hasSize(4);
    }


}