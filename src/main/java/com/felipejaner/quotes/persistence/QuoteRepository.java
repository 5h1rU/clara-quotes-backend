package com.felipejaner.quotes.persistence;

import com.felipejaner.quotes.domain.Quote;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {
  @Override
  @EntityGraph(attributePaths = "conditions")
  List<Quote> findAll(Sort sort);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select q from Quote q where q.id = :id")
  Optional<Quote> findForUpdate(@Param("id") UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update Quote q set q.status = com.felipejaner.quotes.domain.QuoteStatus.EXPIRED, q.updatedAt = :now, q.version = q.version + 1 where q.status = com.felipejaner.quotes.domain.QuoteStatus.DRAFT and q.createdAt < :cutoff")
  int expireDrafts(@Param("cutoff") Instant cutoff, @Param("now") Instant now);
}
