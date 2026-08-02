package com.regisx001.dQul.datasource.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regisx001.dQul.connector.ConnectorFactory;
import com.regisx001.dQul.dataset.repository.DatasetRepository;
import com.regisx001.dQul.datasource.domain.Datasource;
import com.regisx001.dQul.datasource.domain.DatasourceStatus;
import com.regisx001.dQul.datasource.repository.DatasourceRepository;

@ExtendWith(MockitoExtension.class)
class DatasourceServiceImplTest {

    @Mock
    private DatasourceRepository datasourceRepository;

    @Mock
    private DatasetRepository datasetRepository;

    @Mock
    private ConnectorFactory connectorFactory;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DatasourceServiceImpl datasourceService;

    @Test
    @DisplayName("createDatasource allows PostgreSQL type")
    void createDatasource_allowsPostgres() {
        when(datasourceRepository.existsByName("pg-ds")).thenReturn(false);
        when(datasourceRepository.save(any(Datasource.class))).thenAnswer(inv -> inv.getArgument(0));

        Datasource ds = datasourceService.createDatasource("pg-ds", "PostgreSQL", "desc", "owner");

        assertNotNull(ds);
        assertEquals("PostgreSQL", ds.getType());
        verify(datasourceRepository).save(any(Datasource.class));
    }

    @Test
    @DisplayName("createDatasource allows CSV type")
    void createDatasource_allowsCsv() {
        when(datasourceRepository.existsByName("csv-ds")).thenReturn(false);
        when(datasourceRepository.save(any(Datasource.class))).thenAnswer(inv -> inv.getArgument(0));

        Datasource ds = datasourceService.createDatasource("csv-ds", "CSV", "desc", "owner");

        assertNotNull(ds);
        assertEquals("CSV", ds.getType());
        verify(datasourceRepository).save(any(Datasource.class));
    }

    @Test
    @DisplayName("createDatasource rejects unsupported connector types")
    void createDatasource_rejectsUnsupportedTypes() {
        when(datasourceRepository.existsByName("mysql-ds")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                datasourceService.createDatasource("mysql-ds", "MySQL", "desc", "owner")
        );

        assertTrue(ex.getMessage().contains("Unsupported datasource type"));
        verify(datasourceRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateDatasource rejects updating to an unsupported connector type")
    void updateDatasource_rejectsUnsupportedType() {
        UUID id = UUID.randomUUID();
        Datasource existing = Datasource.builder()
                .id(id)
                .name("my-ds")
                .type("PostgreSQL")
                .status(DatasourceStatus.ACTIVE)
                .build();

        when(datasourceRepository.findById(id)).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                datasourceService.updateDatasource(id, null, "Oracle", null, null)
        );

        assertTrue(ex.getMessage().contains("Unsupported datasource type"));
        verify(datasourceRepository, never()).save(any());
    }
}
