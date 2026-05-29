package txnflow.paymentadapterservice.service.internal;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import txnflow.paymentadapterservice.entity.OutboxEvent;
import txnflow.paymentadapterservice.entity.PaymentLedger;
import txnflow.paymentadapterservice.enums.OutboxEventType;
import txnflow.paymentadapterservice.enums.OutboxStatus;
import txnflow.paymentadapterservice.enums.PaymentStatus;
import txnflow.paymentadapterservice.exception.PaymentException;
import txnflow.paymentadapterservice.properties.RazorpayProperties;
import txnflow.paymentadapterservice.repository.OutboxRepository;
import txnflow.paymentadapterservice.repository.PaymentLedgerRepository;
import txnflow.paymentadapterservice.service.RazorpayWebhookService;

import static txnflow.paymentadapterservice.constant.RazorpayConstant.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultRazorpayWebhookService implements RazorpayWebhookService {

    private final RazorpayProperties razorpayProperties;
    private final PaymentLedgerRepository paymentLedgerRepository;
    private final OutboxRepository outboxRepository;

    @Override
    @Transactional
    public void handleWebhook(String payload, String signature) {

        verifySignature(payload, signature);

        JSONObject jsonObject = new JSONObject(payload);

        String event = jsonObject.getString(EVENT);
        log.info("Razorpay webhook received. event={}", event);

        PaymentLedger ledger = fetchLedger(jsonObject);

        dispatchEvent(event, ledger);

        log.info("Razorpay webhook processed. event={}", event);
    }

    // ---------------- CORE DISPATCH ----------------

    private void dispatchEvent(String event, PaymentLedger ledger) {

        switch (event) {

            case PAYMENT_CAPTURED -> handlePaymentCaptured(ledger);

            case PAYMENT_FAILED -> handlePaymentFailed(ledger);

            default -> log.info("Unhandled event={}", event);
        }
    }

    // ---------------- SUCCESS FLOW ----------------

    private void handlePaymentCaptured(PaymentLedger ledger) {

        if (ledger.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Already SUCCESS ledgerId={}", ledger.getId());
            return;
        }

        markSuccess(ledger);
        createOutboxIfNotExists(ledger);

        log.info("Payment SUCCESS ledgerId={}", ledger.getId());
    }

    private void markSuccess(PaymentLedger ledger) {
        ledger.setStatus(PaymentStatus.SUCCESS);
        paymentLedgerRepository.save(ledger);
    }

    private void createOutboxIfNotExists(PaymentLedger ledger) {

        boolean exists = outboxRepository.existsByLedgerIdAndEventType(
                ledger.getId(),
                OutboxEventType.WALLET_CREDIT
        );

        if (exists) return;

        OutboxEvent event = OutboxEvent.builder()
                .eventType(OutboxEventType.WALLET_CREDIT)
                .userId(ledger.getUserId())
                .ledgerId(ledger.getId())
                .amount(ledger.getAmount())
                .status(OutboxStatus.PENDING)
                .build();

        outboxRepository.save(event);
    }

    // ---------------- FAILURE FLOW ----------------

    private void handlePaymentFailed(PaymentLedger ledger) {

        if (ledger.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Ignoring FAILED, already SUCCESS ledgerId={}", ledger.getId());
            return;
        }

        markFailed(ledger);

        log.info("Payment FAILED ledgerId={}", ledger.getId());
    }

    private void markFailed(PaymentLedger ledger) {
        ledger.setStatus(PaymentStatus.FAILED);
        paymentLedgerRepository.save(ledger);
    }

    // ---------------- COMMON HELPERS ----------------

    private void verifySignature(String payload, String signature) {
        try {
            boolean isValid = Utils.verifyWebhookSignature(
                    payload,
                    signature,
                    razorpayProperties.webhookSecret()
            );

            if (!isValid) {
                throw new PaymentException("Invalid webhook signature");
            }

        } catch (RazorpayException ex) {
            throw new PaymentException("Webhook verification failed", ex);
        }
    }

    private PaymentLedger fetchLedger(JSONObject jsonObject) {

        JSONObject paymentEntity = jsonObject
                .getJSONObject(PAYLOAD)
                .getJSONObject(PAYMENT)
                .getJSONObject(ENTITY);

        String razorpayOrderId = paymentEntity.getString(ORDER_ID);

        return paymentLedgerRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> {
                    log.error("Ledger not found orderId={}", razorpayOrderId);
                    return new PaymentException("Ledger not found");
                });
    }
}