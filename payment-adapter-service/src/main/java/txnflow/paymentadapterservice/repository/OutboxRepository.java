package txnflow.paymentadapterservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import txnflow.paymentadapterservice.entity.OutboxEvent;
import txnflow.paymentadapterservice.enums.OutboxEventType;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
                SELECT id
                FROM outbox_event
                WHERE status = :status
                ORDER BY created_at ASC
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> fetchPendingIds(@Param("status") String status, @Param("limit") int limit);

    boolean existsByLedgerIdAndEventType(UUID ledgerId, OutboxEventType eventType);
}