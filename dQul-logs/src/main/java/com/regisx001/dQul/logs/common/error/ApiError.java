package com.regisx001.dQul.logs.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Consistent API error envelope returned by the {@link GlobalExceptionHandler}.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> fieldErrors;
}
