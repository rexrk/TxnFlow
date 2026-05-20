package txnflow.walletservice.transfer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import txnflow.walletservice.transfer.dto.request.TransferMoneyRequest;
import txnflow.walletservice.transfer.dto.response.TransferMoneyResponse;
import txnflow.walletservice.transfer.service.TransferService;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferMoneyResponse transferMoney(
            @Valid @RequestBody TransferMoneyRequest request
    ) {
        return transferService.transferMoney(request);
    }
}