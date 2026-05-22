package txnflow.walletservice.transaction.dto.response;

import java.util.UUID;

public record CounterpartyDetails(
        UUID userId,
        String name,
        String email
) {
}