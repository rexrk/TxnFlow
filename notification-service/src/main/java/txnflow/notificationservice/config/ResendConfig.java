package txnflow.notificationservice.config;

import com.resend.Resend;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import txnflow.notificationservice.properties.ResendProperties;

@Configuration
public class ResendConfig {

    @Bean
    public Resend resend(ResendProperties properties) {
        return new Resend(properties.apiKey());
    }
}
