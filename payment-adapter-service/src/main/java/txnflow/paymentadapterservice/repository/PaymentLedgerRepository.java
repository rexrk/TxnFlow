package txnflow.paymentadapterservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import txnflow.paymentadapterservice.entity.PaymentLedger;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentLedgerRepository extends JpaRepository<PaymentLedger, UUID> {

    Optional<PaymentLedger> findByRazorpayOrderId(String razorpayOrderId);
    Optional<PaymentLedger> findByIdAndUserId(UUID id, UUID userId);
}