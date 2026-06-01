package txnflow.notificationservice.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Manual integration test")
class ResendIntegrationTest {

    @Autowired
    private Resend resend;

    @Test
    void shouldSendEmail() {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("noreply@mail.txnflow.dpdns.org")
                .to("ramankumar4272@gmail.com")
                .subject("it works!")
                .html("<strong>hello world</strong>")
                .build();

        try {
            CreateEmailResponse data = resend.emails().send(params);
            System.out.println(data.getId());
        } catch (ResendException e) {
            e.printStackTrace();
        }
    }

}