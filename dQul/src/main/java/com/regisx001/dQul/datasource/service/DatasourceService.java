package com.regisx001.dQul.datasource.service;

import java.util.List;
import java.util.UUID;

import com.regisx001.dQul.connector.api.ConnectionTestResult;
import com.regisx001.dQul.connector.api.DatasetDescriptor;
import com.regisx001.dQul.dataset.domain.Dataset;
import com.regisx001.dQul.datasource.domain.Datasource;
import com.regisx001.dQul.datasource.domain.DatasourceStatus;

public interface DatasourceService {

        // ── CRUD ──────────────────────────────────────────────────────────────

        Datasource createDatasource(String name, String type, String description,
                        String owner);

        Datasource createDatasource(String name, String type, String description,
                        String owner, String configJson);

        Datasource getDatasourceById(UUID id);

        Datasource getDatasourceByName(String name);

        List<Datasource> getAllDatasources();

        Datasource updateDatasource(UUID id, String name, String type,
                        String description, DatasourceStatus status);

        void deleteDatasource(UUID id);

        // ── Queries ───────────────────────────────────────────────────────────

        List<Datasource> getDatasourcesByStatus(DatasourceStatus status);

        List<Datasource> getDatasourcesByOwner(String owner);

        // ── Status management ────────────────────────────────────────────────

        Datasource activateDatasource(UUID id);

        Datasource disableDatasource(UUID id);

        Datasource archiveDatasource(UUID id);

        // ── Helpers ───────────────────────────────────────────────────────────

        boolean isNameTaken(String name);

        // ── Configuration ────────────────────────────────────────────────────

        /**
         * Saves or updates the JSON configuration for a datasource.
         *
         * @param id         the datasource ID
         * @param configJson JSON string with connector-specific configuration
         * @return the updated datasource
         */
        Datasource saveConfiguration(UUID id, String configJson);

        /**
         * Retrieves the JSON configuration for a datasource.
         *
         * @param id the datasource ID
         * @return the JSON configuration string, or {@code null} if not set
         */
        String getConfiguration(UUID id);

        // ── Connection Testing ──────────────────────────────────────────────

        /**
         * Tests the connection for a datasource using its stored configuration.
         *
         * @param id the datasource ID
         * @return the connection test result
         */
        ConnectionTestResult testConnection(UUID id);

        // ── Dataset Discovery & Import ──────────────────────────────────────

        /**
         * Discovers available datasets (tables/views/files) from the datasource.
         *
         * @param id the datasource ID
         * @return list of dataset descriptors exposed by the connector
         */
        List<DatasetDescriptor> discoverDatasets(UUID id);

        /**
         * Imports selected discovered datasets into managed dataset entities.
         *
         * @param id         the datasource ID
         * @param datasetIds list of dataset IDs/names to import
         * @return list of created or updated dataset entities
         */
        List<Dataset> importDatasets(UUID id, List<String> datasetIds);
}
