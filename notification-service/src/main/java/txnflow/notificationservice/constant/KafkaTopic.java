package txnflow.notificationservice.constant;

public final class KafkaTopic {
    private KafkaTopic() {}

    // =========================
    // USER EVENTS
    // =========================
    public static final String USER_REGISTERED = "user.registered";
    public static final String USER_VERIFIED = "user.verified";
    public static final String USER_DELETED = "user.deleted";

    // =========================
    // WALLET EVENTS
    // =========================
    public static final String WALLET_CREATED = "wallet.created";
    public static final String WALLET_CREDITED = "wallet.credited";
    public static final String WALLET_DEBITED = "wallet.debited";

    // =========================
    // TOPUP EVENTS
    // =========================
    public static final String TOPUP_INITIATED = "topup.initiated";
    public static final String TOPUP_COMPLETED = "topup.completed";
    public static final String TOPUP_FAILED = "topup.failed";

    // =========================
    // TRANSFER EVENTS
    // =========================
    public static final String TRANSFER_INITIATED = "transfer.initiated";
    public static final String TRANSFER_COMPLETED = "transfer.completed";
    public static final String TRANSFER_FAILED = "transfer.failed";

    // =========================
    // OTP / SECURITY EVENTS
    // =========================
    public static final String OTP_REQUESTED = "otp.requested";
    public static final String OTP_VERIFIED = "otp.verified";

}
