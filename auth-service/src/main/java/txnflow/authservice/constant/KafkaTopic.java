package txnflow.authservice.constant;

public final class KafkaTopic {
    private KafkaTopic() {}

    // =========================
    // USER EVENTS
    // =========================
    public static final String USER_REGISTERED = "user.registered";
    public static final String USER_VERIFIED = "user.verified";
    public static final String USER_DELETED = "user.deleted";

    // =========================
    // OTP / SECURITY EVENTS
    // =========================
    public static final String OTP_REQUESTED = "otp.requested";
    public static final String OTP_VERIFIED = "otp.verified";

}
