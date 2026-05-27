package txnflow.paymentadapterservice.dto.response;

public record PaymentCheckoutResponse(
        String key,
        String orderId,
        Long amount,
        String currency
) {
}