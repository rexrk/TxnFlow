package txnflow.walletservice.orchestration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import txnflow.walletservice.constant.Currency;
import txnflow.walletservice.exception.InsufficientBalanceException;
import txnflow.walletservice.exception.InvalidTransferException;
import txnflow.walletservice.exception.WalletNotFoundException;
import txnflow.walletservice.kafka.event.TransferCompletedEvent;
import txnflow.walletservice.transaction.entity.WalletTransaction;
import txnflow.walletservice.transaction.enums.TransactionCategory;
import txnflow.walletservice.transaction.enums.TransactionType;
import txnflow.walletservice.transaction.repository.WalletTransactionRepository;
import txnflow.walletservice.transfer.dto.request.TransferMoneyRequest;
import txnflow.walletservice.transfer.dto.response.TransferMoneyResponse;
import txnflow.walletservice.transfer.entity.WalletTransfer;
import txnflow.walletservice.transfer.enums.TransferStatus;
import txnflow.walletservice.transfer.mapper.TransferMapper;
import txnflow.walletservice.transfer.repository.WalletTransferRepository;
import txnflow.walletservice.wallet.entity.Wallet;
import txnflow.walletservice.wallet.enums.WalletStatus;
import txnflow.walletservice.wallet.repository.WalletRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static txnflow.walletservice.kafka.constant.KafkaTopic.TOPUP_COMPLETED;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferProcessor {

    private final WalletRepository walletRepository;
    private final WalletTransferRepository walletTransferRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransferMapper transferMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public TransferMoneyResponse processTransfer(
            String senderEmail,
            TransferMoneyRequest request,
            String requestFingerprint
    ) {
        Wallet senderWallet = walletRepository.findByEmailForUpdate(senderEmail)
                .orElseThrow(() -> {
                    log.warn("Transfer processing failed: sender wallet not found. senderEmail={}", senderEmail);
                    return new WalletNotFoundException("Sender wallet not found");
                });

        verifyWalletPin(senderWallet, request.walletPin());

        Wallet receiverWallet = walletRepository.findByEmailForUpdate(request.receiverEmail())
                .orElseThrow(() -> {
                    log.warn("Transfer processing failed: receiver wallet not found. receiverEmail={}",
                            request.receiverEmail());
                    return new WalletNotFoundException("Receiver wallet not found");
                });

        validateWalletActive(senderWallet, "Sender wallet is not active");
        validateWalletActive(receiverWallet, "Receiver wallet is not active");

        if (senderWallet.getBalance().compareTo(request.amount()) < 0) {
            log.warn("Transfer rejected: insufficient balance. senderWalletId={}", senderWallet.getId());
            throw new InsufficientBalanceException("Insufficient wallet balance");
        }

        WalletTransfer transfer = WalletTransfer.builder()
                .idempotencyKey(request.idempotencyKey())
                .requestFingerprint(requestFingerprint)
                .senderWalletId(senderWallet.getId())
                .receiverWalletId(receiverWallet.getId())
                .amount(request.amount())
                .currency(Currency.INR)
                .status(TransferStatus.PROCESSING)
                .description(request.description())
                .build();

        transfer = walletTransferRepository.saveAndFlush(transfer);

        BigDecimal senderBalanceAfter =
                senderWallet.getBalance().subtract(request.amount());

        BigDecimal receiverBalanceAfter =
                receiverWallet.getBalance().add(request.amount());

        senderWallet.setBalance(senderBalanceAfter);
        receiverWallet.setBalance(receiverBalanceAfter);

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        WalletTransaction debitTransaction = walletTransactionRepository.save(
                WalletTransaction.builder()
                        .walletId(senderWallet.getId())
                        .transferId(transfer.getId())
                        .type(TransactionType.DEBIT)
                        .amount(request.amount())
                        .balanceAfter(senderBalanceAfter)
                        .currency(Currency.INR)
                        .description(request.description())
                        .counterpartyUserId(receiverWallet.getId())
                        .category(TransactionCategory.PAID)
                        .build()
        );

        WalletTransaction creditTransaction = walletTransactionRepository.save(
                WalletTransaction.builder()
                        .walletId(receiverWallet.getId())
                        .transferId(transfer.getId())
                        .type(TransactionType.CREDIT)
                        .amount(request.amount())
                        .balanceAfter(receiverBalanceAfter)
                        .currency(Currency.INR)
                        .description("Payment received")
                        .counterpartyUserId(senderWallet.getId())
                        .category(TransactionCategory.RECEIVED)
                        .build()
        );

        transfer.setDebitTransactionId(debitTransaction.getId());
        transfer.setCreditTransactionId(creditTransaction.getId());
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCompletedAt(Instant.now());

        WalletTransfer completedTransfer = walletTransferRepository.saveAndFlush(transfer);

        TransferCompletedEvent transferCompletedEvent =
                new TransferCompletedEvent(
                        UUID.randomUUID(),
                        senderWallet.getId(),
                        completedTransfer.getId(),
                        senderEmail,
                        request.amount().longValue(),
                        request.receiverEmail()
                );

        kafkaTemplate.send(TOPUP_COMPLETED, senderWallet.getId().toString(), transferCompletedEvent);

        log.info("Transfer completed. transferId={} senderWalletId={} receiverWalletId={}",
                completedTransfer.getId(),
                senderWallet.getId(),
                receiverWallet.getId());

        return transferMapper.toTransferMoneyResponse(completedTransfer, false);
    }

    private void validateWalletActive(Wallet wallet, String message) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            log.warn("Transfer rejected: wallet is not active. walletId={}", wallet.getId());
            throw new InvalidTransferException(message);
        }
    }

    private void verifyWalletPin(Wallet wallet, String rawPin) {
        if (!wallet.isPinSet()) {
            log.warn("Transfer rejected: wallet PIN is not set. walletId={}", wallet.getId());
            throw new InvalidTransferException("Wallet PIN is not set");
        }

        if (!passwordEncoder.matches(rawPin, wallet.getPinHash())) {
            log.warn("Transfer rejected: invalid wallet PIN. walletId={}", wallet.getId());
            throw new InvalidTransferException("Invalid wallet PIN");
        }
    }

}
