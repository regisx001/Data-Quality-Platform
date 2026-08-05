package com.regisx001.dQul.logs.dto;

import com.regisx001.dQul.logs.domain.LogEntry;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Paginated response envelope for querying logs. Wraps the content plus
 * pagination metadata so clients can page through results without parsing
 * raw Spring {@link org.springframework.data.domain.Page} internals.
 */
@Data
@Builder
public class LogPageDto {
    private List<LogEntry> content;
    private int page; // current page (0-based)
    private int size; // page size requested
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;
}
