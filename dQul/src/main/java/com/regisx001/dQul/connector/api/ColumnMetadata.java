package com.regisx001.dQul.connector.api;

/**
 * Describes a single column or field within a dataset.
 *
 * @param name      The column name as it appears in the datasource
 * @param type      The canonical {@link DataType} of the column
 * @param nullable  Whether the column allows {@code NULL} values
 * @param precision The numeric precision for decimal types, or {@code null}
 * @param scale     The numeric scale for decimal types, or {@code null}
 */
public record ColumnMetadata(
        String name,
        DataType type,
        boolean nullable,
        Integer precision,
        Integer scale) {

    public ColumnMetadata(String name, DataType type, boolean nullable) {
        this(name, type, nullable, null, null);
    }
}