package com.regisx001.dQul.dataset.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DatasetServiceImplProfilingTest {

    @Test
    @DisplayName("isNullValue should return true for null, string 'null', empty strings, 'none', and 'n/a'")
    void testIsNullValue() {
        assertTrue(DatasetServiceImpl.isNullValue(null));
        assertTrue(DatasetServiceImpl.isNullValue(""));
        assertTrue(DatasetServiceImpl.isNullValue("   "));
        assertTrue(DatasetServiceImpl.isNullValue("null"));
        assertTrue(DatasetServiceImpl.isNullValue("NULL"));
        assertTrue(DatasetServiceImpl.isNullValue("Null"));
        assertTrue(DatasetServiceImpl.isNullValue("   null   "));
        assertTrue(DatasetServiceImpl.isNullValue("none"));
        assertTrue(DatasetServiceImpl.isNullValue("NONE"));
        assertTrue(DatasetServiceImpl.isNullValue("n/a"));
        assertTrue(DatasetServiceImpl.isNullValue("N/A"));

        assertFalse(DatasetServiceImpl.isNullValue("valid_value"));
        assertFalse(DatasetServiceImpl.isNullValue("123"));
        assertFalse(DatasetServiceImpl.isNullValue("0"));
        assertFalse(DatasetServiceImpl.isNullValue("false"));
    }

    @Test
    @DisplayName("isMinMaxComputable should return true for numeric & date/time types and false for string/uuid/json/unknown")
    void testIsMinMaxComputable() {
        assertTrue(DatasetServiceImpl.isMinMaxComputable("INTEGER"));
        assertTrue(DatasetServiceImpl.isMinMaxComputable("BIGINT"));
        assertTrue(DatasetServiceImpl.isMinMaxComputable("DOUBLE"));
        assertTrue(DatasetServiceImpl.isMinMaxComputable("FLOAT"));
        assertTrue(DatasetServiceImpl.isMinMaxComputable("TIMESTAMP"));
        assertTrue(DatasetServiceImpl.isMinMaxComputable("DATE"));
        assertTrue(DatasetServiceImpl.isMinMaxComputable("LONG"));

        assertFalse(DatasetServiceImpl.isMinMaxComputable("STRING"));
        assertFalse(DatasetServiceImpl.isMinMaxComputable("VARCHAR"));
        assertFalse(DatasetServiceImpl.isMinMaxComputable("TEXT"));
        assertFalse(DatasetServiceImpl.isMinMaxComputable("JSON"));
        assertFalse(DatasetServiceImpl.isMinMaxComputable("UUID"));
        assertFalse(DatasetServiceImpl.isMinMaxComputable("UNKNOWN"));
        assertFalse(DatasetServiceImpl.isMinMaxComputable("BOOLEAN"));
        assertFalse(DatasetServiceImpl.isMinMaxComputable(null));
    }
}
