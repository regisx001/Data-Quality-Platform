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

import com.regisx001.dQul.common.responses.ApiErrorResponse;
import com.regisx001.dQul.connector.ConnectorConfigSchema;
import com.regisx001.dQul.connector.ConnectorConfigSchemaService;
import com.regisx001.dQul.connector.api.ConnectionTestResult;
import com.regisx001.dQul.connector.api.DatasetDescriptor;
import com.regisx001.dQul.dataset.domain.Dataset;
import com.regisx001.dQul.datasource.domain.Datasource;
import com.regisx001.dQul.datasource.domain.DatasourceStatus;
import com.regisx001.dQul.datasource.service.DatasourceService;
import com.regisx001.dQul.storage.minio.MinioStorageService;

import jakarta.persistence.EntityNotFoundException;

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
    public ResponseEntity<?> uploadCsvFile(@RequestParam("file") MultipartFile file) {
        try {
            MinioStorageService.FileUploadResult result = minioStorageService.uploadCsvFile(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Failed to upload CSV file: " + e.getMessage())
                            .build());
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> createDatasource(@RequestBody CreateDatasourceRequest request) {
        try {
            Datasource datasource = datasourceService.createDatasource(
                    request.name(),
                    request.type(),
                    request.description(),
                    request.owner(),
                    request.configJson());
            return ResponseEntity.status(HttpStatus.CREATED).body(datasource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping
    public ResponseEntity<List<Datasource>> getAllDatasources() {
        return ResponseEntity.ok(datasourceService.getAllDatasources());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDatasourceById(@PathVariable UUID id) {
        try {
            Datasource datasource = datasourceService.getDatasourceById(id);
            return ResponseEntity.ok(datasource);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<?> getDatasourceByName(@PathVariable String name) {
        try {
            Datasource datasource = datasourceService.getDatasourceByName(name);
            return ResponseEntity.ok(datasource);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDatasource(@PathVariable UUID id,
            @RequestBody UpdateDatasourceRequest request) {
        try {
            Datasource updated = datasourceService.updateDatasource(
                    id, request.name(), request.type(),
                    request.description(), request.status());
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDatasource(@PathVariable UUID id) {
        try {
            datasourceService.deleteDatasource(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
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
    public ResponseEntity<?> activateDatasource(@PathVariable UUID id) {
        try {
            Datasource datasource = datasourceService.activateDatasource(id);
            return ResponseEntity.ok(datasource);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<?> disableDatasource(@PathVariable UUID id) {
        try {
            Datasource datasource = datasourceService.disableDatasource(id);
            return ResponseEntity.ok(datasource);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<?> archiveDatasource(@PathVariable UUID id) {
        try {
            Datasource datasource = datasourceService.archiveDatasource(id);
            return ResponseEntity.ok(datasource);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    // ── Configuration Schemas ────────────────────────────────────────────

    @GetMapping("/config-schemas")
    public ResponseEntity<List<ConnectorConfigSchema>> getConfigSchemas() {
        return ResponseEntity.ok(configSchemaService.getAllSchemas());
    }

    @GetMapping("/{type}/config-schema")
    public ResponseEntity<?> getConfigSchemaByType(@PathVariable String type) {
        ConnectorConfigSchema schema = configSchemaService.getSchema(type);
        if (schema == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message("No config schema found for type: " + type)
                            .build());
        }
        return ResponseEntity.ok(schema);
    }

    // ── Configuration ────────────────────────────────────────────────────

    @PutMapping("/{id}/config")
    public ResponseEntity<?> saveConfiguration(
            @PathVariable UUID id,
            @RequestBody SaveConfigRequest request) {
        try {
            Datasource datasource = datasourceService.saveConfiguration(
                    id, request.configJson());
            return ResponseEntity.ok(datasource);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{id}/config")
    public ResponseEntity<?> getConfiguration(@PathVariable UUID id) {
        try {
            String config = datasourceService.getConfiguration(id);
            if (config == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(
                    new SaveConfigRequest(config));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    // ── Connection Testing ──────────────────────────────────────────────

    @PostMapping("/{id}/test-connection")
    public ResponseEntity<?> testConnection(@PathVariable UUID id) {
        try {
            ConnectionTestResult result = datasourceService.testConnection(id);
            return ResponseEntity.ok(result);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    // ── Dataset Discovery & Import ───────────────────────────────────────

    @GetMapping("/{id}/discover-datasets")
    public ResponseEntity<?> discoverDatasets(@PathVariable UUID id) {
        try {
            List<DatasetDescriptor> datasets = datasourceService.discoverDatasets(id);
            return ResponseEntity.ok(datasets);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Failed to discover datasets: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/{id}/import-datasets")
    public ResponseEntity<?> importDatasets(
            @PathVariable UUID id,
            @RequestBody ImportDatasetsRequest request) {
        try {
            List<Dataset> imported = datasourceService.importDatasets(id, request.datasetIds());
            return ResponseEntity.ok(imported);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .build());
        }
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
