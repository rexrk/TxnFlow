package txnflow.paymentadapterservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import txnflow.paymentadapterservice.dto.request.CreateOrderRequest;
import txnflow.paymentadapterservice.dto.response.CreateOrderResponse;
import txnflow.paymentadapterservice.dto.response.PaymentCheckoutResponse;
import txnflow.paymentadapterservice.service.PaymentService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/topup")
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(paymentService.createOrder(request));

    }

    @GetMapping("/{ledgerId}")
    public ResponseEntity<PaymentCheckoutResponse> getPaymentCheckoutDetails(
            @NotNull @PathVariable UUID ledgerId
    ) {
        return ResponseEntity.ok(paymentService.getPaymentCheckoutDetails(ledgerId));
    }

}