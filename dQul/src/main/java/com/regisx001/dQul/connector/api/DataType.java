package com.regisx001.dQul.connector.api;

/**
 * Represents the platform-level data type of a column or field within a
 * dataset.
 * Connectors translate their native type systems into these canonical types,
 * ensuring that the rest of the platform can reason about column types without
 * knowledge of the underlying datasource.
 */
public enum DataType {

    /** Variable-length character string. */
    STRING,

    /** 32-bit signed integer. */
    INTEGER,

    /** 64-bit signed integer. */
    LONG,

    /** Double-precision floating point (64-bit). */
    DOUBLE,

    /** High-precision decimal number with configurable scale. */
    DECIMAL,

    /** Logical boolean value (true / false). */
    BOOLEAN,

    /** Calendar date (year, month, day) without time component. */
    DATE,

    /** Timestamp with or without time zone. */
    TIMESTAMP,

    /** Binary large object or fixed-length binary data. */
    BINARY,

    /** Array or list of elements. */
    ARRAY,

    /** Structured / nested record type. */
    STRUCT,

    /** Any type that does not map to a known canonical type. */
    UNKNOWN
}
