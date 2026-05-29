package txnflow.paymentadapterservice.service;

public interface RazorpayWebhookService {
    void handleWebhook(String payload, String signature);
}