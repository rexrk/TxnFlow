package txnflow.paymentadapterservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import txnflow.paymentadapterservice.properties.RazorpayProperties;

@EnableScheduling
@EnableConfigurationProperties(RazorpayProperties.class)
@SpringBootApplication
public class PaymentAdapterServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentAdapterServiceApplication.class, args);
	}

}
