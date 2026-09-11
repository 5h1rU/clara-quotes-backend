package com.felipejaner.quotes.messaging;


import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.UUID;
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = "select * from outbox_events where published_at is null order by occurred_at limit 20 for update skip locked", nativeQuery = true)
    List<OutboxEvent> lockPendingBatch();
}
