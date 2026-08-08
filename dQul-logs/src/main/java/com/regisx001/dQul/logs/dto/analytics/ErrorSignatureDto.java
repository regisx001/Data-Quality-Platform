package com.regisx001.dQul.logs.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Normalized recurring error signature. Stack traces and messages with high
 * cardinality instance details are collapsed into a stable fingerprint so
 * thousands of raw errors reduce to a small set of signatures.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorSignatureDto {

    private String signature;
    private long count;
    private double percentage;
    private long firstOccurrenceEpochMillis;
    private long lastOccurrenceEpochMillis;
    private String exampleMessage;
    private String exampleStackTrace;
}
