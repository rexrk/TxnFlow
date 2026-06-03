package txnflow.notificationservice.template;

import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import txnflow.notificationservice.properties.ResendProperties;

@Component
@RequiredArgsConstructor
public class EmailTemplateFactory {

    private final ResendProperties resendProperties;

    private CreateEmailOptions build(String email, String subject, String body) {
        return CreateEmailOptions.builder()
                .from(resendProperties.email())
                .to(email)
                .subject(subject)
                .html(body)
                .build();
    }

    public CreateEmailOptions userRegistered(String email) {
        String subject = "Welcome to TxnFlow";

        String body = """
                <h2>Welcome to TxnFlow</h2>
                <p>Your account has been successfully registered.</p>
                """;

        return build(email, subject, body);

    }

    public CreateEmailOptions topupComplete(String email, Long amount) {
        String subject = "Topup Complete";

        String body = """
            <h2>TxnFlow Topup</h2>
            <p>Your wallet has been credited with ₹%s.</p>
            """.formatted(amount);

        return build(email, subject, body);
    }

    public CreateEmailOptions topupFailed(String email, Long amount) {
        String subject = "Topup Complete";

        String body = """
            <h2>TxnFlow Topup</h2>
            <p>Topup failed for amount ₹%s.</p>
            """.formatted(amount);

        return build(email, subject, body);
    }
}