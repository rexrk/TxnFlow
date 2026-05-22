package txnflow.walletservice.transaction.mapper;

import org.springframework.stereotype.Component;
import txnflow.walletservice.transaction.dto.response.CounterpartyDetails;
import txnflow.walletservice.transaction.dto.response.WalletTransactionListItemResponse;
import txnflow.walletservice.transaction.dto.response.WalletTransactionResponse;
import txnflow.walletservice.transaction.entity.WalletTransaction;

@Component
public class TransactionMapper {

    public WalletTransactionResponse toWalletTransactionResponse(
            WalletTransaction transaction
    ) {
        return new WalletTransactionResponse(
                transaction.getId(),
                transaction.getTransferId(),
                transaction.getType(),
                transaction.getCategory(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getCurrency(),
                transaction.getDescription(),
                new CounterpartyDetails(transaction.getCounterpartyUserId(), null, null),
                transaction.getCreatedAt()
        );
    }

    public WalletTransactionListItemResponse toWalletTransactionListItemResponse(
            WalletTransaction transaction
    ) {
        return new WalletTransactionListItemResponse(
                transaction.getId(),
                transaction.getTransferId(),
                transaction.getCounterpartyUserId(),
                transaction.getCategory(),
                transaction.getAmount(),
                transaction.getCreatedAt()
        );
    }
}