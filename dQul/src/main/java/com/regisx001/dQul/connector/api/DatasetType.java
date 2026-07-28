package com.regisx001.dQul.connector.api;

/**
 * Categorises the kind of dataset that a connector exposes.
 * The platform uses this information to decide how to interact with
 * and present datasets from heterogeneous datasources.
 */
public enum DatasetType {

    /** A relational database table. */
    TABLE,

    /** A relational database view (virtual table). */
    VIEW,

    /** A NoSQL collection or document store. */
    COLLECTION,

    /** A flat file (CSV, Parquet, JSON, etc.). */
    FILE,

    /** The result of a SQL or other query expression. */
    QUERY,

    /** A type that cannot be classified into one of the above categories. */
    UNKNOWN
}
