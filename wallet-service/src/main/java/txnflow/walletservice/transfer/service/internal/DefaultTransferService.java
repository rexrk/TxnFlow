package txnflow.walletservice.transfer.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import txnflow.walletservice.constant.Currency;
import txnflow.walletservice.exception.InsufficientBalanceException;
import txnflow.walletservice.exception.InvalidTransferException;
import txnflow.walletservice.exception.WalletNotFoundException;
import txnflow.walletservice.security.CurrentUserProvider;
import txnflow.walletservice.transaction.entity.WalletTransaction;
import txnflow.walletservice.transaction.enums.TransactionType;
import txnflow.walletservice.transaction.repository.WalletTransactionRepository;
import txnflow.walletservice.transfer.dto.request.TransferMoneyRequest;
import txnflow.walletservice.transfer.dto.response.TransferMoneyResponse;
import txnflow.walletservice.transfer.entity.WalletTransfer;
import txnflow.walletservice.transfer.enums.TransferStatus;
import txnflow.walletservice.transfer.repository.WalletTransferRepository;
import txnflow.walletservice.transfer.service.TransferService;
import txnflow.walletservice.wallet.entity.Wallet;
import txnflow.walletservice.wallet.enums.WalletStatus;
import txnflow.walletservice.wallet.repository.WalletRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultTransferService implements TransferService {

    private final WalletRepository walletRepository;
    private final WalletTransferRepository walletTransferRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public TransferMoneyResponse transferMoney(TransferMoneyRequest request) {
        UUID senderUserId = currentUserProvider.getCurrentAppUserId();

        if (senderUserId.equals(request.receiverUserId())) {
            throw new InvalidTransferException("Cannot transfer money to yourself");
        }

        return walletTransferRepository.findByIdempotencyKey(request.idempotencyKey())
                .map(this::toResponse)
                .orElseGet(() -> processTransfer(senderUserId, request));
    }

    private TransferMoneyResponse processTransfer(UUID senderUserId, TransferMoneyRequest request) {
        Wallet senderWallet = walletRepository.findByUserIdForUpdate(senderUserId)
                .orElseThrow(() -> new WalletNotFoundException("Sender wallet not found"));

        verifyWalletPin(senderWallet, request.walletPin());

        Wallet receiverWallet = walletRepository.findByUserIdForUpdate(request.receiverUserId())
                .orElseThrow(() -> new WalletNotFoundException("Receiver wallet not found"));

        validateWalletActive(senderWallet, "Sender wallet is not active");
        validateWalletActive(receiverWallet, "Receiver wallet is not active");

        if (senderWallet.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient wallet balance");
        }

        WalletTransfer transfer = WalletTransfer.builder()
                .idempotencyKey(request.idempotencyKey())
                .senderWalletId(senderWallet.getId())
                .receiverWalletId(receiverWallet.getId())
                .amount(request.amount())
                .currency(Currency.INR)
                .status(TransferStatus.PROCESSING)
                .description(request.description())
                .build();

        transfer = walletTransferRepository.save(transfer);

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
                        .description("Transfer sent")
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
                        .description("Transfer received")
                        .build()
        );

        transfer.setDebitTransactionId(debitTransaction.getId());
        transfer.setCreditTransactionId(creditTransaction.getId());
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCompletedAt(Instant.now());

        WalletTransfer completedTransfer = walletTransferRepository.save(transfer);

        return toResponse(completedTransfer);
    }

    private void validateWalletActive(Wallet wallet, String message) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new InvalidTransferException(message);
        }
    }

    private void verifyWalletPin(Wallet wallet, String rawPin) {
        if (!wallet.isPinSet()) {
            throw new InvalidTransferException("Wallet PIN is not set");
        }

        if (!passwordEncoder.matches(rawPin, wallet.getPinHash())) {
            throw new InvalidTransferException("Invalid wallet PIN");
        }
    }

    private TransferMoneyResponse toResponse(WalletTransfer transfer) {
        return new TransferMoneyResponse(
                transfer.getId(),
                transfer.getStatus(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getSenderWalletId(),
                transfer.getReceiverWalletId(),
                transfer.getDebitTransactionId(),
                transfer.getCreditTransactionId(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt()
        );
    }
}