package com.regisx001.dQul.logs.repository;

import com.regisx001.dQul.logs.domain.RealtimeLogMetricEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RealtimeLogMetricRepository extends JpaRepository<RealtimeLogMetricEntity, UUID> {

    @Query("SELECT r FROM RealtimeLogMetricEntity r ORDER BY r.createdAt DESC")
    List<RealtimeLogMetricEntity> findRecentMetrics(Pageable pageable);
}
