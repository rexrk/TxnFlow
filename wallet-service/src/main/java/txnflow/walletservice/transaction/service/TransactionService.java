package txnflow.walletservice.transaction.service;

import txnflow.walletservice.transaction.dto.response.WalletTransactionListItemResponse;
import txnflow.walletservice.transaction.dto.response.WalletTransactionResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionService {
    WalletTransactionResponse getTransaction(UUID transactionId);
    List<WalletTransactionListItemResponse> getRecentTransactions();
    List<WalletTransactionResponse> getMyTransactions(LocalDate from, LocalDate to);

}
