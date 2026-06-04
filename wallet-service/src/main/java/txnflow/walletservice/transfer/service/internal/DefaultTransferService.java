package txnflow.walletservice.transfer.service.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import txnflow.walletservice.constant.Currency;
import txnflow.walletservice.exception.IdempotencyConflictException;
import txnflow.walletservice.exception.InvalidTransferException;
import txnflow.walletservice.exception.WalletNotFoundException;
import txnflow.walletservice.orchestration.TransferProcessor;
import txnflow.walletservice.security.CurrentUserProvider;
import txnflow.walletservice.transfer.dto.request.TransferMoneyRequest;
import txnflow.walletservice.transfer.dto.response.TransferMoneyResponse;
import txnflow.walletservice.transfer.entity.WalletTransfer;
import txnflow.walletservice.transfer.mapper.TransferMapper;
import txnflow.walletservice.transfer.repository.WalletTransferRepository;
import txnflow.walletservice.transfer.service.TransferService;
import txnflow.walletservice.wallet.repository.WalletRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTransferService implements TransferService {

    private final WalletRepository walletRepository;
    private final WalletTransferRepository walletTransferRepository;
    private final TransferProcessor transferProcessor;
    private final CurrentUserProvider currentUserProvider;
    private final TransferMapper transferMapper;

    @Override
    public TransferMoneyResponse transferMoney(TransferMoneyRequest request) {
        UUID senderUserId = currentUserProvider.getCurrentAppUserId();
        String senderEmail = currentUserProvider.getCurrentUserEmail();

        if (senderEmail.equals(request.receiverEmail())) {
            log.warn("Transfer rejected: sender and receiver are same. userId={}", senderUserId);
            throw new InvalidTransferException("Cannot transfer money to yourself");
        }

        UUID senderWalletId = walletRepository.findWalletIdByUserId(senderUserId)
                .orElseThrow(() -> {
                    log.warn("Transfer rejected: sender wallet not found. senderUserId={}", senderUserId);
                    return new WalletNotFoundException("Sender wallet not found");
                });

        String requestFingerprint = buildRequestFingerprint(senderUserId, request);

        Optional<WalletTransfer> existingTransfer =
                walletTransferRepository.findBySenderWalletIdAndIdempotencyKey(
                        senderWalletId,
                        request.idempotencyKey()
                );

        if (existingTransfer.isPresent()) {
            validateIdempotentRequest(existingTransfer.get(), requestFingerprint);
            log.info("Idempotent transfer replayed. transferId={}", existingTransfer.get().getId());
            return transferMapper.toTransferMoneyResponse(existingTransfer.get(), true);
        }

        log.info("Transfer requested. senderWalletId={} receiverUserId={}", senderWalletId, request.receiverEmail());

        try {
            return transferProcessor.processTransfer(senderEmail, request, requestFingerprint);

        } catch (DataIntegrityViolationException ex ) {
            log.warn("Duplicate idempotency race detected. senderWalletId={}", senderWalletId);

            WalletTransfer transfer = walletTransferRepository
                    .findBySenderWalletIdAndIdempotencyKey(senderWalletId, request.idempotencyKey())
                    .orElseThrow(() -> new InvalidTransferException("Concurrent transfer failed"));

            validateIdempotentRequest(transfer, requestFingerprint);
            return transferMapper.toTransferMoneyResponse(transfer, true);
        }
    }

    private String buildRequestFingerprint(
            UUID senderUserId,
            TransferMoneyRequest request
    ) {
        try {
            String raw = senderUserId + "|" +
                    request.receiverEmail() + "|" +
                    request.amount().stripTrailingZeros().toPlainString() + "|" +
                    Currency.INR;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate request fingerprint", ex);
        }
    }

    private void validateIdempotentRequest(
            WalletTransfer existingTransfer,
            String requestFingerprint
    ) {
        if (!existingTransfer.getRequestFingerprint().equals(requestFingerprint)) {
            log.warn("Idempotency conflict detected. transferId={}", existingTransfer.getId());
            throw new IdempotencyConflictException(
                    "Idempotency key already used with different request"
            );

        }
    }

}
