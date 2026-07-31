package com.regisx001.dQul.connector.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataTypeTest {

    @Test
    @DisplayName("All expected enum constants exist")
    void enumConstants() {
        assertAll(
                () -> assertEquals(DataType.STRING, DataType.valueOf("STRING")),
                () -> assertEquals(DataType.INTEGER, DataType.valueOf("INTEGER")),
                () -> assertEquals(DataType.LONG, DataType.valueOf("LONG")),
                () -> assertEquals(DataType.DOUBLE, DataType.valueOf("DOUBLE")),
                () -> assertEquals(DataType.DECIMAL, DataType.valueOf("DECIMAL")),
                () -> assertEquals(DataType.BOOLEAN, DataType.valueOf("BOOLEAN")),
                () -> assertEquals(DataType.DATE, DataType.valueOf("DATE")),
                () -> assertEquals(DataType.TIMESTAMP, DataType.valueOf("TIMESTAMP")),
                () -> assertEquals(DataType.BINARY, DataType.valueOf("BINARY")),
                () -> assertEquals(DataType.ARRAY, DataType.valueOf("ARRAY")),
                () -> assertEquals(DataType.STRUCT, DataType.valueOf("STRUCT")),
                () -> assertEquals(DataType.UNKNOWN, DataType.valueOf("UNKNOWN")));
    }

    @Test
    @DisplayName("All DatasetType enum constants exist")
    void datasetTypeConstants() {
        assertAll(
                () -> assertEquals(DatasetType.TABLE, DatasetType.valueOf("TABLE")),
                () -> assertEquals(DatasetType.VIEW, DatasetType.valueOf("VIEW")),
                () -> assertEquals(DatasetType.COLLECTION, DatasetType.valueOf("COLLECTION")),
                () -> assertEquals(DatasetType.FILE, DatasetType.valueOf("FILE")),
                () -> assertEquals(DatasetType.QUERY, DatasetType.valueOf("QUERY")),
                () -> assertEquals(DatasetType.UNKNOWN, DatasetType.valueOf("UNKNOWN")));
    }
}
