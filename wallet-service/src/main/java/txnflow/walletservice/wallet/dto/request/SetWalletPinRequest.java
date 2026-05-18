package txnflow.walletservice.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetWalletPinRequest(
        @NotBlank(message = "pin is required")
        @Pattern(regexp = "\\d{4}", message = "pin must be 4 digits")
        String pin
) {
}