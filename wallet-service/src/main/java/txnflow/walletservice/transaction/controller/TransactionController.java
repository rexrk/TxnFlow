package txnflow.walletservice.transaction.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import txnflow.walletservice.transaction.dto.response.WalletTransactionListItemResponse;
import txnflow.walletservice.transaction.dto.response.WalletTransactionResponse;
import txnflow.walletservice.transaction.service.TransactionService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/me/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService walletTransactionService;

    @GetMapping("/{transactionId}")
    public WalletTransactionResponse getTransaction(
            @PathVariable UUID transactionId
    ) {
        return walletTransactionService.getTransaction(transactionId);
    }

    @GetMapping("/recent")
    public List<WalletTransactionListItemResponse> getRecentTransactions() {
        return walletTransactionService.getRecentTransactions();
    }

    @GetMapping
    public List<WalletTransactionResponse> getMyTransactions(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        return walletTransactionService.getMyTransactions(from, to);
    }

}
