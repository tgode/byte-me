package com.bytehr.repository;

import com.bytehr.model.Analytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AnalyticsRepository extends JpaRepository<Analytics, UUID> {

    @Query("SELECT COUNT(a) FROM Analytics a WHERE a.wasAnswered = true")
    long countAnswered();

    @Query("SELECT AVG(a.responseTimeMs) FROM Analytics a WHERE a.responseTimeMs IS NOT NULL")
    Double averageResponseTimeMs();

    @Query("SELECT AVG(a.confidenceScore) FROM Analytics a WHERE a.confidenceScore IS NOT NULL")
    Double averageConfidenceScore();
}
