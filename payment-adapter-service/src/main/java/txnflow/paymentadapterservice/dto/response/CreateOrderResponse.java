package txnflow.paymentadapterservice.dto.response;

public record CreateOrderResponse(
        String orderId,
        Long amount,
        String currency,
        String status,
        String checkoutUrl
) {}