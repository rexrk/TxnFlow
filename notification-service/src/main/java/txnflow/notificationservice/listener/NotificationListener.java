package txnflow.notificationservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import txnflow.notificationservice.dto.event.UserRegisteredEvent;
import txnflow.notificationservice.dto.request.NotificationRequest;
import txnflow.notificationservice.enums.NotificationType;
import txnflow.notificationservice.service.EmailService;
import txnflow.notificationservice.template.EmailTemplateFactory;

import static txnflow.notificationservice.constant.KafkaTopic.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final EmailService emailService;
    private final EmailTemplateFactory emailTemplateFactory;

    // =========================
    // USER SERVICE
    // =========================

    @KafkaListener(topics = USER_REGISTERED)
    public void onUserRegistered(@Payload UserRegisteredEvent event) {
        emailService.send(
                new NotificationRequest(
                        event.eventId(),
                        event.userId(),
                        event.keycloakUserId(),
                        event.email(),
                        NotificationType.WELCOME,
                        emailTemplateFactory.userRegistered(event.email())
                )
        );

    }

//    @KafkaListener(topics = USER_VERIFIED)
//    public void onUserVerified(UserVerifiedEvent event) {
//    }


    // =========================
    // WALLET SERVICE
    // =========================

//    @KafkaListener(topics = USER_REGISTERED)
//    public void createWalletOnUserRegistration(UserRegisteredEvent event) {
//    }
//
//    @KafkaListener(topics = WALLET_CREDITED)
//    public void onWalletCredited(WalletCreditedEvent event) {
//    }
//
//    @KafkaListener(topics = WALLET_DEBITED)
//    public void onWalletDebited(WalletDebitedEvent event) {
//    }


    // =========================
    // TOPUP SERVICE
    // =========================

//    @KafkaListener(topics = TOPUP_INITIATED)
//    public void onTopupInitiated(TopupInitiatedEvent event) {
//    }
//
//    @KafkaListener(topics = TOPUP_COMPLETED)
//    public void onTopupCompleted(TopupCompletedEvent event) {
//    }
//
//    @KafkaListener(topics = TOPUP_FAILED)
//    public void onTopupFailed(TopupFailedEvent event) {
//    }


    // =========================
    // TRANSFER SERVICE
    // =========================

//    @KafkaListener(topics = TRANSFER_INITIATED)
//    public void onTransferInitiated(TransferInitiatedEvent event) {
//    }
//
//    @KafkaListener(topics = TRANSFER_COMPLETED)
//    public void onTransferCompleted(TransferCompletedEvent event) {
//    }
//
//    @KafkaListener(topics = TRANSFER_FAILED)
//    public void onTransferFailed(TransferFailedEvent event) {
//    }


    // =========================
    // NOTIFICATION SERVICE
    // =========================

//    @KafkaListener(topics = USER_REGISTERED)
//    public void onUserRegistered(UserRegisteredEvent event) {
//    }
//
//    @KafkaListener(topics = TOPUP_COMPLETED)
//    public void onTopupCompleted(TopupCompletedEvent event) {
//    }
//
//    @KafkaListener(topics = TRANSFER_COMPLETED)
//    public void onTransferCompleted(TransferCompletedEvent event) {
//    }
//
//    @KafkaListener(topics = OTP_REQUESTED)
//    public void onOtpRequested(OtpRequestedEvent event) {
//    }


}