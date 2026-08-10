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

    // ── SQL Aggregation Queries for Database-Level Analytics ─────────────

    @Query("SELECT l.logLevel, COUNT(l) FROM LogEntry l " +
           "WHERE l.timestamp >= :fromNorm AND l.timestamp <= :toNorm " +
           "AND (:serviceName IS NULL OR LOWER(l.serviceName) = LOWER(CAST(:serviceName AS string))) " +
           "AND (:category IS NULL OR UPPER(l.category) = UPPER(CAST(:category AS string))) " +
           "AND (:traceId IS NULL OR l.traceId = CAST(:traceId AS string)) " +
           "GROUP BY l.logLevel")
    List<Object[]> countLogsByLevelAggregated(
            @Param("fromNorm") Instant fromNorm,
            @Param("toNorm") Instant toNorm,
            @Param("serviceName") String serviceName,
            @Param("category") String category,
            @Param("traceId") String traceId);

    @Query("SELECT l.serviceName, COUNT(l), " +
           "SUM(CASE WHEN UPPER(l.logLevel) IN ('ERROR', 'FATAL') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN UPPER(l.logLevel) = 'FATAL' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN UPPER(l.logLevel) = 'WARN' THEN 1 ELSE 0 END), " +
           "AVG(l.executionTimeMs), MAX(l.executionTimeMs) " +
           "FROM LogEntry l " +
           "WHERE l.timestamp >= :fromNorm AND l.timestamp <= :toNorm " +
           "AND (:serviceName IS NULL OR LOWER(l.serviceName) = LOWER(CAST(:serviceName AS string))) " +
           "AND (:category IS NULL OR UPPER(l.category) = UPPER(CAST(:category AS string))) " +
           "AND (:traceId IS NULL OR l.traceId = CAST(:traceId AS string)) " +
           "GROUP BY l.serviceName")
    List<Object[]> aggregateServices(
            @Param("fromNorm") Instant fromNorm,
            @Param("toNorm") Instant toNorm,
            @Param("serviceName") String serviceName,
            @Param("category") String category,
            @Param("traceId") String traceId);

    @Query("SELECT l.category, COUNT(l), " +
           "SUM(CASE WHEN UPPER(l.logLevel) IN ('ERROR', 'FATAL') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN UPPER(l.logLevel) = 'FATAL' THEN 1 ELSE 0 END), " +
           "AVG(l.executionTimeMs) " +
           "FROM LogEntry l " +
           "WHERE l.timestamp >= :fromNorm AND l.timestamp <= :toNorm " +
           "AND (:serviceName IS NULL OR LOWER(l.serviceName) = LOWER(CAST(:serviceName AS string))) " +
           "AND (:category IS NULL OR UPPER(l.category) = UPPER(CAST(:category AS string))) " +
           "AND (:traceId IS NULL OR l.traceId = CAST(:traceId AS string)) " +
           "GROUP BY l.category")
    List<Object[]> aggregateCategories(
            @Param("fromNorm") Instant fromNorm,
            @Param("toNorm") Instant toNorm,
            @Param("serviceName") String serviceName,
            @Param("category") String category,
            @Param("traceId") String traceId);

    @Query("SELECT l.executionTimeMs FROM LogEntry l " +
           "WHERE l.timestamp >= :fromNorm AND l.timestamp <= :toNorm " +
           "AND (:serviceName IS NULL OR LOWER(l.serviceName) = LOWER(CAST(:serviceName AS string))) " +
           "AND (:category IS NULL OR UPPER(l.category) = UPPER(CAST(:category AS string))) " +
           "AND (:traceId IS NULL OR l.traceId = CAST(:traceId AS string)) " +
           "AND (:targetService IS NULL OR LOWER(l.serviceName) = LOWER(CAST(:targetService AS string))) " +
           "AND (:targetCategory IS NULL OR UPPER(l.category) = UPPER(CAST(:targetCategory AS string))) " +
           "AND (:targetMethod IS NULL OR UPPER(l.httpMethod) = UPPER(CAST(:targetMethod AS string))) " +
           "AND (:targetPath IS NULL OR l.path = CAST(:targetPath AS string)) " +
           "AND l.executionTimeMs IS NOT NULL")
    List<Long> findLatencies(
            @Param("fromNorm") Instant fromNorm,
            @Param("toNorm") Instant toNorm,
            @Param("serviceName") String serviceName,
            @Param("category") String category,
            @Param("traceId") String traceId,
            @Param("targetService") String targetService,
            @Param("targetCategory") String targetCategory,
            @Param("targetMethod") String targetMethod,
            @Param("targetPath") String targetPath);

    @Query("SELECT l.statusCode, COUNT(l) FROM LogEntry l " +
           "WHERE l.timestamp >= :fromNorm AND l.timestamp <= :toNorm " +
           "AND (:serviceName IS NULL OR LOWER(l.serviceName) = LOWER(CAST(:serviceName AS string))) " +
           "AND (:category IS NULL OR UPPER(l.category) = UPPER(CAST(:category AS string))) " +
           "AND (:traceId IS NULL OR l.traceId = CAST(:traceId AS string)) " +
           "AND l.statusCode IS NOT NULL " +
           "GROUP BY l.statusCode")
    List<Object[]> aggregateHttpStatusCodes(
            @Param("fromNorm") Instant fromNorm,
            @Param("toNorm") Instant toNorm,
            @Param("serviceName") String serviceName,
            @Param("category") String category,
            @Param("traceId") String traceId);

    @Query("SELECT UPPER(TRIM(l.httpMethod)), COUNT(l) FROM LogEntry l " +
           "WHERE l.timestamp >= :fromNorm AND l.timestamp <= :toNorm " +
           "AND (:serviceName IS NULL OR LOWER(l.serviceName) = LOWER(CAST(:serviceName AS string))) " +
           "AND (:category IS NULL OR UPPER(l.category) = UPPER(CAST(:category AS string))) " +
           "AND (:traceId IS NULL OR l.traceId = CAST(:traceId AS string)) " +
           "AND l.httpMethod IS NOT NULL AND TRIM(l.httpMethod) <> '' " +
           "GROUP BY UPPER(TRIM(l.httpMethod))")
    List<Object[]> aggregateHttpMethodCounts(
            @Param("fromNorm") Instant fromNorm,
            @Param("toNorm") Instant toNorm,
            @Param("serviceName") String serviceName,
            @Param("category") String category,
            @Param("traceId") String traceId);

    @Query("SELECT UPPER(TRIM(COALESCE(l.httpMethod, 'ANY'))), l.path, COUNT(l), " +
           "SUM(CASE WHEN l.statusCode >= 400 THEN 1 ELSE 0 END), " +
           "AVG(l.executionTimeMs), MAX(l.executionTimeMs) " +
           "FROM LogEntry l " +
           "WHERE l.timestamp >= :fromNorm AND l.timestamp <= :toNorm " +
           "AND (:serviceName IS NULL OR LOWER(l.serviceName) = LOWER(CAST(:serviceName AS string))) " +
           "AND (:category IS NULL OR UPPER(l.category) = UPPER(CAST(:category AS string))) " +
           "AND (:traceId IS NULL OR l.traceId = CAST(:traceId AS string)) " +
           "AND l.path IS NOT NULL " +
           "GROUP BY UPPER(TRIM(COALESCE(l.httpMethod, 'ANY'))), l.path")
    List<Object[]> aggregateHttpEndpoints(
            @Param("fromNorm") Instant fromNorm,
            @Param("toNorm") Instant toNorm,
            @Param("serviceName") String serviceName,
            @Param("category") String category,
            @Param("traceId") String traceId);

    @Query("SELECT l.userId, MAX(l.userEmail), COUNT(l), " +
           "SUM(CASE WHEN UPPER(l.logLevel) IN ('ERROR', 'FATAL') THEN 1 ELSE 0 END) " +
           "FROM LogEntry l " +
           "WHERE l.timestamp >= :fromNorm AND l.timestamp <= :toNorm " +
           "AND (:serviceName IS NULL OR LOWER(l.serviceName) = LOWER(CAST(:serviceName AS string))) " +
           "AND (:category IS NULL OR UPPER(l.category) = UPPER(CAST(:category AS string))) " +
           "AND (:traceId IS NULL OR l.traceId = CAST(:traceId AS string)) " +
           "AND l.userId IS NOT NULL AND TRIM(l.userId) <> '' " +
           "GROUP BY l.userId")
    List<Object[]> aggregateUsers(
            @Param("fromNorm") Instant fromNorm,
            @Param("toNorm") Instant toNorm,
            @Param("serviceName") String serviceName,
            @Param("category") String category,
            @Param("traceId") String traceId);
}
