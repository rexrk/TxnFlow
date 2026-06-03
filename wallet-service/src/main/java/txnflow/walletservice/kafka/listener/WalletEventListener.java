package txnflow.walletservice.kafka.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import txnflow.walletservice.kafka.constant.KafkaTopic;
import txnflow.walletservice.kafka.event.TopupCompletedEvent;
import txnflow.walletservice.kafka.event.UserRegisteredEvent;
import txnflow.walletservice.orchestration.TopupProcessor;
import txnflow.walletservice.wallet.service.WalletService;

import static txnflow.walletservice.kafka.constant.KafkaTopic.TOPUP_COMPLETED;
import static txnflow.walletservice.kafka.constant.KafkaTopic.USER_REGISTERED;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletEventListener {

    private final WalletService walletService;
    private final TopupProcessor topupProcessor;

    @KafkaListener(topics = USER_REGISTERED)
    public void onUserRegistered(UserRegisteredEvent event) {
        walletService.createWalletForUser(event.userId());

        log.info("Wallet creation processed. userId={}", event.userId());

    }

    @KafkaListener(topics = TOPUP_COMPLETED)
    public void onTopupCompleted(TopupCompletedEvent event) {
        topupProcessor.processTopup(event);
        log.info("Topup processed. userId={}", event.userId());

    }
}