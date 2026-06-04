package txnflow.walletservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("test")
public class TestKafkaConfig {

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        // Return a Mockito mock so any send() calls in tests are no-ops
        return mock(KafkaTemplate.class);
    }
}
