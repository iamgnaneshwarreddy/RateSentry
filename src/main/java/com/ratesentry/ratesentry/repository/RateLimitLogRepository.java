package com.ratesentry.ratesentry.repository;

import com.ratesentry.ratesentry.model.RateLimitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RateLimitLogRepository extends JpaRepository<RateLimitLog, Long> {
    List<RateLimitLog> findByClientId(String clientId);
    List<RateLimitLog> findByClientIdAndEndpoint(String clientId, String endpoint);
    List<RateLimitLog> findByAllowedFalse();
}