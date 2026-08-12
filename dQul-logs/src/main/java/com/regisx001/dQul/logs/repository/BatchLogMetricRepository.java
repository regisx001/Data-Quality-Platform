package com.regisx001.dQul.logs.repository;

import com.regisx001.dQul.logs.domain.BatchLogMetricEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchLogMetricRepository extends JpaRepository<BatchLogMetricEntity, UUID> {

    Optional<BatchLogMetricEntity> findByJobId(UUID jobId);

    @Query("SELECT b FROM BatchLogMetricEntity b ORDER BY b.createdAt DESC")
    List<BatchLogMetricEntity> findRecentBatchMetrics(Pageable pageable);
}
