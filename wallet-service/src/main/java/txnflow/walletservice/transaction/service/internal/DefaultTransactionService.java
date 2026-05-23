package txnflow.walletservice.transaction.service.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import txnflow.walletservice.exception.WalletTransactionNotFoundException;
import txnflow.walletservice.transaction.dto.response.WalletTransactionListItemResponse;
import txnflow.walletservice.transaction.dto.response.WalletTransactionResponse;
import txnflow.walletservice.transaction.entity.WalletTransaction;
import txnflow.walletservice.transaction.mapper.TransactionMapper;
import txnflow.walletservice.transaction.repository.WalletTransactionRepository;
import txnflow.walletservice.transaction.service.TransactionService;
import txnflow.walletservice.wallet.service.WalletService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultTransactionService implements TransactionService {

    private final TransactionMapper transactionMapper;
    private final WalletService walletService;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    public WalletTransactionResponse getTransaction(UUID transactionId) {
        UUID walletId = walletService.getCurrentWalletId();

        WalletTransaction transaction = walletTransactionRepository.findById(transactionId)
                .orElseThrow(() -> {
                    log.warn("Transaction lookup failed. transactionId={} walletId={}", transactionId, walletId);
                    return new WalletTransactionNotFoundException("Transaction not found");
                });

        if (!transaction.getWalletId().equals(walletId)) {
            log.warn("Transaction access rejected. transactionId={} walletId={}", transactionId, walletId);
            throw new WalletTransactionNotFoundException("Transaction not found");
        }

        return transactionMapper.toWalletTransactionResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionListItemResponse> getRecentTransactions() {
        UUID walletId = walletService.getCurrentWalletId();

        List<WalletTransactionListItemResponse> transactions = walletTransactionRepository
                .findTop20ByWalletIdOrderByCreatedAtDesc(walletId)
                .stream()
                .map(transactionMapper::toWalletTransactionListItemResponse)
                .toList();

        log.info("Recent transactions fetched. walletId={} count={}", walletId, transactions.size());
        return transactions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getMyTransactions(
            LocalDate from,
            LocalDate to
    ) {
        UUID walletId = walletService.getCurrentWalletId();
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<WalletTransactionResponse> transactions = walletTransactionRepository
                .findByWalletIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        walletId,
                        fromInstant,
                        toInstant
                )
                .stream()
                .map(transactionMapper::toWalletTransactionResponse)
                .toList();

        log.info("Transactions fetched. walletId={} count={} from={} to={}",
                walletId,
                transactions.size(),
                from,
                to);
        return transactions;
    }

}
