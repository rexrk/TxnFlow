package txnflow.walletservice.transfer.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferMoneyRequest(

        // [Fix]: Enable later
//        @NotBlank(message = "receiverEmail is required")
//        @Email(message = "invalid email")
//        String receiverEmail,

        @NotNull(message = "receiverUserId is required")
        UUID receiverUserId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        @Digits(integer = 15, fraction = 4)
        BigDecimal amount,

        @NotBlank(message = "idempotencyKey is required")
        String idempotencyKey,

        @NotBlank(message = "walletPin is required")
        @Pattern(regexp = "\\d{4}", message = "walletPin must be 4 digits")
        String walletPin,

        String description

) {
}