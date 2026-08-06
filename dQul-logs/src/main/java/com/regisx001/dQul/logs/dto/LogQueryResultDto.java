package com.regisx001.dQul.logs.dto;

import com.regisx001.dQul.logs.domain.LogEntry;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Cache-friendly page of query results.
 *
 * <p>This is deliberately NOT a Spring {@link org.springframework.data.domain.Page}
 * / {@code PageImpl}, because that polymorphic wrapper does not round-trip cleanly
 * through Redis JSON serialization (its type id is denied by the configured
 * {@code PolymorphicTypeValidator}, causing
 * {@code SerializationException} on cache reads). Instead we cache just the
 * concrete content list plus the total element count, which preserves all
 * pagination metadata the {@link LogPageDto} envelope needs without pulling the
 * {@code PageImpl} internals (sort orders, etc.) into Redis.
 */
@Data
@Builder
public class LogQueryResultDto {
    private List<LogEntry> content;
    private long totalElements;
}
