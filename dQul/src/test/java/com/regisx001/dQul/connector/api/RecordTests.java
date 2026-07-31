package com.regisx001.dQul.connector.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ColumnMetadataTest {

    @Test
    @DisplayName("Full constructor sets all fields")
    void fullConstructor() {
        var col = new ColumnMetadata("id", DataType.INTEGER, false, 10, 0);

        assertEquals("id", col.name());
        assertEquals(DataType.INTEGER, col.type());
        assertFalse(col.nullable());
        assertEquals(10, col.precision());
        assertEquals(0, col.scale());
    }

    @Test
    @DisplayName("Convenience constructor defaults precision and scale to null")
    void convenienceConstructor() {
        var col = new ColumnMetadata("name", DataType.STRING, true);

        assertEquals("name", col.name());
        assertEquals(DataType.STRING, col.type());
        assertTrue(col.nullable());
        assertNull(col.precision());
        assertNull(col.scale());
    }
}

class DatasetDescriptorTest {

    @Test
    @DisplayName("Full constructor sets all fields")
    void fullConstructor() {
        var desc = new DatasetDescriptor("schema.table", "table", DatasetType.TABLE, "A test table");

        assertEquals("schema.table", desc.id());
        assertEquals("table", desc.name());
        assertEquals(DatasetType.TABLE, desc.type());
        assertEquals("A test table", desc.description());
    }

    @Test
    @DisplayName("Convenience constructor defaults description to null")
    void convenienceConstructor() {
        var desc = new DatasetDescriptor("file.csv", "file", DatasetType.FILE);

        assertEquals("file.csv", desc.id());
        assertEquals("file", desc.name());
        assertEquals(DatasetType.FILE, desc.type());
        assertNull(desc.description());
    }
}

class DatasetMetadataTest {

    @Test
    @DisplayName("Record stores all fields correctly")
    void recordFields() {
        var columns = List.of(
                new ColumnMetadata("id", DataType.INTEGER, false),
                new ColumnMetadata("name", DataType.STRING, true));
        var meta = new DatasetMetadata("users", columns, 1000L);

        assertEquals("users", meta.name());
        assertEquals(2, meta.columns().size());
        assertEquals(1000L, meta.estimatedRows());
    }
}
