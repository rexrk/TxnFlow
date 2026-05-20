package txnflow.walletservice.transfer.mapper;

import org.springframework.stereotype.Component;
import txnflow.walletservice.transfer.dto.response.TransferMoneyResponse;
import txnflow.walletservice.transfer.entity.WalletTransfer;

@Component
public class TransferMapper {

    public TransferMoneyResponse toTransferMoneyResponse(WalletTransfer transfer, boolean idempotentReplay) {
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
                transfer.getCompletedAt(),
                idempotentReplay
        );
    }
}