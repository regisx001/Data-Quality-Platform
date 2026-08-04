package com.regisx001.dQul.logs.repository;

import com.regisx001.dQul.logs.domain.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, UUID>, JpaSpecificationExecutor<LogEntry> {

    Page<LogEntry> findByLogLevel(String logLevel, Pageable pageable);

    Page<LogEntry> findByServiceName(String serviceName, Pageable pageable);

    Page<LogEntry> findByTraceId(String traceId, Pageable pageable);

    long countByLogLevel(String logLevel);

    long countByTimestampAfter(Instant timestamp);

    @Query("SELECT AVG(l.executionTimeMs) FROM LogEntry l WHERE l.executionTimeMs IS NOT NULL AND l.timestamp >= :since")
    Double getAverageExecutionTimeSince(@Param("since") Instant since);

    @Query("SELECT l.serviceName, COUNT(l) FROM LogEntry l GROUP BY l.serviceName")
    List<Object[]> countLogsByService();

    @Query("SELECT l.category, COUNT(l) FROM LogEntry l GROUP BY l.category")
    List<Object[]> countLogsByCategory();

    void deleteByTimestampBefore(Instant timestamp);
}
