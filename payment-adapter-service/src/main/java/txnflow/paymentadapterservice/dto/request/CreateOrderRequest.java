package txnflow.paymentadapterservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull
        @Min(10)
        Long amount
) {}