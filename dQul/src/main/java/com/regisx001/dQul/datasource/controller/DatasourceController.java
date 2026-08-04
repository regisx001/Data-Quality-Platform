package com.regisx001.dQul.datasource.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.regisx001.dQul.connector.ConnectorConfigSchema;
import com.regisx001.dQul.connector.ConnectorConfigSchemaService;
import com.regisx001.dQul.connector.api.ConnectionTestResult;
import com.regisx001.dQul.connector.api.DatasetDescriptor;
import com.regisx001.dQul.connector.exception.ConnectorNotFoundException;
import com.regisx001.dQul.dataset.domain.Dataset;
import com.regisx001.dQul.datasource.domain.Datasource;
import com.regisx001.dQul.datasource.domain.DatasourceStatus;
import com.regisx001.dQul.datasource.service.DatasourceService;
import com.regisx001.dQul.storage.minio.MinioStorageService;

@RestController
@RequestMapping("/api/v1/datasources")
public class DatasourceController {

    private final DatasourceService datasourceService;
    private final ConnectorConfigSchemaService configSchemaService;
    private final MinioStorageService minioStorageService;

    public DatasourceController(DatasourceService datasourceService,
            ConnectorConfigSchemaService configSchemaService,
            MinioStorageService minioStorageService) {
        this.datasourceService = datasourceService;
        this.configSchemaService = configSchemaService;
        this.minioStorageService = minioStorageService;
    }

    // ── CSV File Upload (MinIO) ───────────────────────────────────────────

    @PostMapping(value = "/upload-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MinioStorageService.FileUploadResult> uploadCsvFile(@RequestParam("file") MultipartFile file) {
        MinioStorageService.FileUploadResult result = minioStorageService.uploadCsvFile(file);
        return ResponseEntity.ok(result);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Datasource> createDatasource(@RequestBody CreateDatasourceRequest request) {
        Datasource datasource = datasourceService.createDatasource(
                request.name(),
                request.type(),
                request.description(),
                request.owner(),
                request.configJson());
        return ResponseEntity.status(HttpStatus.CREATED).body(datasource);
    }

    @GetMapping
    public ResponseEntity<List<Datasource>> getAllDatasources() {
        return ResponseEntity.ok(datasourceService.getAllDatasources());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Datasource> getDatasourceById(@PathVariable UUID id) {
        Datasource datasource = datasourceService.getDatasourceById(id);
        return ResponseEntity.ok(datasource);
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<Datasource> getDatasourceByName(@PathVariable String name) {
        Datasource datasource = datasourceService.getDatasourceByName(name);
        return ResponseEntity.ok(datasource);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Datasource> updateDatasource(@PathVariable UUID id,
            @RequestBody UpdateDatasourceRequest request) {
        Datasource updated = datasourceService.updateDatasource(
                id, request.name(), request.type(),
                request.description(), request.status());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDatasource(@PathVariable UUID id) {
        datasourceService.deleteDatasource(id);
        return ResponseEntity.noContent().build();
    }

    // ── Queries ───────────────────────────────────────────────────────────

    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<Datasource>> getDatasourcesByStatus(
            @PathVariable DatasourceStatus status) {
        return ResponseEntity.ok(
                datasourceService.getDatasourcesByStatus(status));
    }

    @GetMapping("/by-owner/{owner}")
    public ResponseEntity<List<Datasource>> getDatasourcesByOwner(
            @PathVariable String owner) {
        return ResponseEntity.ok(
                datasourceService.getDatasourcesByOwner(owner));
    }

    // ── Status management ────────────────────────────────────────────────

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Datasource> activateDatasource(@PathVariable UUID id) {
        Datasource datasource = datasourceService.activateDatasource(id);
        return ResponseEntity.ok(datasource);
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<Datasource> disableDatasource(@PathVariable UUID id) {
        Datasource datasource = datasourceService.disableDatasource(id);
        return ResponseEntity.ok(datasource);
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Datasource> archiveDatasource(@PathVariable UUID id) {
        Datasource datasource = datasourceService.archiveDatasource(id);
        return ResponseEntity.ok(datasource);
    }

    // ── Configuration Schemas ────────────────────────────────────────────

    @GetMapping("/config-schemas")
    public ResponseEntity<List<ConnectorConfigSchema>> getConfigSchemas() {
        return ResponseEntity.ok(configSchemaService.getAllSchemas());
    }

    @GetMapping("/{type}/config-schema")
    public ResponseEntity<ConnectorConfigSchema> getConfigSchemaByType(@PathVariable String type) {
        ConnectorConfigSchema schema = configSchemaService.getSchema(type);
        if (schema == null) {
            throw new ConnectorNotFoundException(type);
        }
        return ResponseEntity.ok(schema);
    }

    // ── Configuration ────────────────────────────────────────────────────

    @PutMapping("/{id}/config")
    public ResponseEntity<Datasource> saveConfiguration(
            @PathVariable UUID id,
            @RequestBody SaveConfigRequest request) {
        Datasource datasource = datasourceService.saveConfiguration(
                id, request.configJson());
        return ResponseEntity.ok(datasource);
    }

    @GetMapping("/{id}/config")
    public ResponseEntity<SaveConfigRequest> getConfiguration(@PathVariable UUID id) {
        String config = datasourceService.getConfiguration(id);
        if (config == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(new SaveConfigRequest(config));
    }

    // ── Connection Testing ──────────────────────────────────────────────

    @PostMapping("/{id}/test-connection")
    public ResponseEntity<ConnectionTestResult> testConnection(@PathVariable UUID id) {
        ConnectionTestResult result = datasourceService.testConnection(id);
        return ResponseEntity.ok(result);
    }

    // ── Dataset Discovery & Import ───────────────────────────────────────

    @GetMapping("/{id}/discover-datasets")
    public ResponseEntity<List<DatasetDescriptor>> discoverDatasets(@PathVariable UUID id) {
        List<DatasetDescriptor> datasets = datasourceService.discoverDatasets(id);
        return ResponseEntity.ok(datasets);
    }

    @PostMapping("/{id}/import-datasets")
    public ResponseEntity<List<Dataset>> importDatasets(
            @PathVariable UUID id,
            @RequestBody ImportDatasetsRequest request) {
        List<Dataset> imported = datasourceService.importDatasets(id, request.datasetIds());
        return ResponseEntity.ok(imported);
    }

    // ── Inner DTOs ───────────────────────────────────────────────────────

    public record CreateDatasourceRequest(
            String name,
            String type,
            String description,
            String owner,
            String configJson) {
    }

    public record UpdateDatasourceRequest(
            String name,
            String type,
            String description,
            DatasourceStatus status) {
    }

    public record SaveConfigRequest(String configJson) {
    }

    public record ImportDatasetsRequest(List<String> datasetIds) {
    }
}
