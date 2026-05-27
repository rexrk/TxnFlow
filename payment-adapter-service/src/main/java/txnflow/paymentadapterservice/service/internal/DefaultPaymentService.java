package txnflow.paymentadapterservice.service.internal;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import txnflow.paymentadapterservice.constant.RazorpayConstant;
import txnflow.paymentadapterservice.dto.request.CreateOrderRequest;
import txnflow.paymentadapterservice.dto.response.CreateOrderResponse;
import txnflow.paymentadapterservice.dto.response.PaymentCheckoutResponse;
import txnflow.paymentadapterservice.entity.PaymentLedger;
import txnflow.paymentadapterservice.enums.PaymentStatus;
import txnflow.paymentadapterservice.exception.PaymentException;
import txnflow.paymentadapterservice.properties.RazorpayProperties;
import txnflow.paymentadapterservice.repository.PaymentLedgerRepository;
import txnflow.paymentadapterservice.security.CurrentUserProvider;
import txnflow.paymentadapterservice.service.PaymentService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultPaymentService implements PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentLedgerRepository paymentLedgerRepository;
    private final CurrentUserProvider userProvider;
    private final RazorpayProperties  razorpayProperties;

    @Override
    public CreateOrderResponse createOrder(CreateOrderRequest request) {

        UUID userId = userProvider.getCurrentAppUserId();

        PaymentLedger paymentLedger = PaymentLedger.builder()
                .userId(userId)
                .amount(request.amount())
                .currency(RazorpayConstant.INR)
                .status(PaymentStatus.CREATED)
                .build();

        paymentLedger = paymentLedgerRepository.save(paymentLedger);

        try {

            JSONObject orderRequest = new JSONObject();

            orderRequest.put(RazorpayConstant.AMOUNT, request.amount() * 100);
            orderRequest.put(RazorpayConstant.CURRENCY, RazorpayConstant.INR);
            orderRequest.put(RazorpayConstant.RECEIPT, paymentLedger.getId().toString());

            Order order = razorpayClient.orders.create(orderRequest);

            paymentLedger.setRazorpayOrderId(order.get(RazorpayConstant.ID));
            paymentLedger.setStatus(PaymentStatus.PENDING);

            paymentLedgerRepository.save(paymentLedger);

            String checkoutUrl =
                    razorpayProperties.checkoutBaseUrl()
                            + "/api/v1/payments/public/razorpay/checkout?ledgerId="
                            + paymentLedger.getId();

            return new CreateOrderResponse(
                    order.get(RazorpayConstant.ID),
                    ((Number) order.get(RazorpayConstant.AMOUNT)).longValue(),
                    order.get(RazorpayConstant.CURRENCY),
                    order.get(RazorpayConstant.STATUS),
                    checkoutUrl
            );

        } catch (RazorpayException ex) {

            paymentLedger.setStatus(PaymentStatus.FAILED);

            paymentLedgerRepository.save(paymentLedger);

            throw new PaymentException("Failed to create order", ex);
        }
    }

    @Override
    public PaymentCheckoutResponse getPaymentCheckoutDetails(UUID ledgerId) {
        UUID currentUserId = userProvider.getCurrentAppUserId();

        PaymentLedger paymentLedger = paymentLedgerRepository
                .findByIdAndUserId(ledgerId, currentUserId)
                .orElseThrow(() ->
                        new PaymentException("Payment ledger not found")
                );

        return new PaymentCheckoutResponse(
                razorpayProperties.keyId(),
                paymentLedger.getRazorpayOrderId(),
                paymentLedger.getAmount(),
                paymentLedger.getCurrency()
        );
    }
}