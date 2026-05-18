package txnflow.walletservice.exception;

public class TransferAlreadyProcessedException extends RuntimeException {
    public TransferAlreadyProcessedException(String message) {
        super(message);
    }
}