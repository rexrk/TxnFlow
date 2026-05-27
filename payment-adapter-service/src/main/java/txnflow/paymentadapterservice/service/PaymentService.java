package txnflow.paymentadapterservice.service;

import txnflow.paymentadapterservice.dto.request.CreateOrderRequest;
import txnflow.paymentadapterservice.dto.response.CreateOrderResponse;
import txnflow.paymentadapterservice.dto.response.PaymentCheckoutResponse;

import java.util.UUID;

public interface PaymentService {
    CreateOrderResponse createOrder(CreateOrderRequest createOrderRequest);
    PaymentCheckoutResponse getPaymentCheckoutDetails(UUID ledgerId);
}
