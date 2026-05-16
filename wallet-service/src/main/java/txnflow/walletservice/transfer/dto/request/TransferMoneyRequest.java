package txnflow.walletservice.transfer.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferMoneyRequest(

        @NotNull(message = "receiverUserId is required")
        UUID receiverUserId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "idempotencyKey is required")
        String idempotencyKey
) {
}