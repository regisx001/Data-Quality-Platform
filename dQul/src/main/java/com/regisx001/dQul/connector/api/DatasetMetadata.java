package com.regisx001.dQul.connector.api;

import java.util.List;

/**
 * Detailed structural metadata about a dataset, including its schema
 * and an estimate of its size.
 */
public record DatasetMetadata(
        String name,
        List<ColumnMetadata> columns,
        long estimatedRows) {
}
