package com.regisx001.dQul.connector.api;

import java.util.List;

/**
 * Detailed structural metadata about a dataset, including its schema
 * and an estimate of its size.
 *
 * @param name          The dataset name
 * @param columns       The ordered list of column definitions
 * @param estimatedRows An approximate row count, or {@code -1} if unknown
 */
public record DatasetMetadata(
        String name,
        List<ColumnMetadata> columns,
        long estimatedRows) {
}