package txnflow.walletservice.kafka.constant;

public class KafkaTopic {
    private KafkaTopic() {}

    public static final String CREATE_WALLET_ON_USER_REGISTRATION = "wallet.create.on-user-registration";
    public static final String CREDIT_WALLET_ON_TOPUP = "wallet.credit.on-topup";
}