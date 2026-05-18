package txnflow.walletservice.transfer.service;

import txnflow.walletservice.transfer.dto.request.TransferMoneyRequest;
import txnflow.walletservice.transfer.dto.response.TransferMoneyResponse;

public interface TransferService {

    TransferMoneyResponse transferMoney(TransferMoneyRequest request);
}