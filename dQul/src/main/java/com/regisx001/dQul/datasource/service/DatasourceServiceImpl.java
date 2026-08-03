package com.regisx001.dQul.datasource.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regisx001.dQul.connector.ConnectorConfig;
import com.regisx001.dQul.connector.ConnectorFactory;
import com.regisx001.dQul.connector.DataSourceConnector;
import com.regisx001.dQul.connector.api.ConnectionTestResult;
import com.regisx001.dQul.connector.api.DatasetDescriptor;
import com.regisx001.dQul.dataset.domain.Dataset;
import com.regisx001.dQul.dataset.domain.DatasetStatus;
import com.regisx001.dQul.dataset.repository.DatasetRepository;
import com.regisx001.dQul.datasource.domain.Datasource;
import com.regisx001.dQul.datasource.domain.DatasourceStatus;
import com.regisx001.dQul.datasource.repository.DatasourceRepository;
import com.regisx001.dQul.dataset.service.DatasetService;
import com.regisx001.dQul.storage.minio.MinioStorageService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;

@Service
@Transactional
public class DatasourceServiceImpl implements DatasourceService {

    private static final Logger log = LoggerFactory.getLogger(DatasourceServiceImpl.class);

    private final DatasourceRepository datasourceRepository;
    private final DatasetRepository datasetRepository;
    private final ConnectorFactory connectorFactory;
    private final ObjectMapper objectMapper;
    private final DatasetService datasetService;
    private final MinioStorageService minioStorageService;

    public DatasourceServiceImpl(DatasourceRepository datasourceRepository,
            DatasetRepository datasetRepository,
            ConnectorFactory connectorFactory,
            ObjectMapper objectMapper,
            @Lazy DatasetService datasetService,
            MinioStorageService minioStorageService) {
        this.datasourceRepository = datasourceRepository;
        this.datasetRepository = datasetRepository;
        this.connectorFactory = connectorFactory;
        this.objectMapper = objectMapper;
        this.datasetService = datasetService;
        this.minioStorageService = minioStorageService;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    @Override
    public Datasource createDatasource(String name, String type, String description,
            String owner) {
        return createDatasource(name, type, description, owner, null);
    }

    @Override
    public Datasource createDatasource(String name, String type, String description,
            String owner, String configJson) {
        if (datasourceRepository.existsByName(name)) {
            throw new IllegalArgumentException(
                    "Datasource with name '" + name + "' already exists");
        }
        validateSupportedType(type);

        Datasource datasource = Datasource.builder()
                .name(name)
                .type(type)
                .description(description)
                .status(DatasourceStatus.REGISTERED)
                .owner(owner)
                .configJson(configJson)
                .registrationDate(LocalDateTime.now())
                .build();

        Datasource saved = datasourceRepository.save(datasource);

        if (configJson != null && !configJson.isBlank()) {
            try {
                ConnectionTestResult testResult = testConnection(saved.getId());
                if (testResult.success()) {
                    saved.setStatus(DatasourceStatus.ACTIVE);
                    saved = datasourceRepository.save(saved);

                    List<DatasetDescriptor> descriptors = discoverDatasets(saved.getId());
                    if (descriptors != null && !descriptors.isEmpty()) {
                        List<String> ids = descriptors.stream().map(DatasetDescriptor::id).toList();
                        importDatasets(saved.getId(), ids);
                    }
                }
            } catch (Exception e) {
                // If auto-connection test or discovery fails, keep status REGISTERED
            }
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Datasource getDatasourceById(UUID id) {
        return datasourceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Datasource not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Datasource getDatasourceByName(String name) {
        return datasourceRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Datasource not found with name: " + name));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Datasource> getAllDatasources() {
        return datasourceRepository.findAll();
    }

    @Override
    public Datasource updateDatasource(UUID id, String name, String type,
            String description, DatasourceStatus status) {
        Datasource datasource = getDatasourceById(id);

        if (name != null && !name.equals(datasource.getName())) {
            if (datasourceRepository.existsByName(name)) {
                throw new IllegalArgumentException(
                        "Datasource with name '" + name + "' already exists");
            }
            datasource.setName(name);
        }
        if (type != null) {
            validateSupportedType(type);
            datasource.setType(type);
        }
        if (description != null) {
            datasource.setDescription(description);
        }
        if (status != null) {
            datasource.setStatus(status);
        }

        return datasourceRepository.save(datasource);
    }

    private void validateSupportedType(String type) {
        if (type == null || (!type.equalsIgnoreCase("POSTGRESQL") && !type.equalsIgnoreCase("CSV"))) {
            throw new IllegalArgumentException(
                    "Unsupported datasource type: " + type + ". Supported connectors are PostgreSQL and CSV.");
        }
    }

    @Override
    public void deleteDatasource(UUID id) {
        Datasource datasource = datasourceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Datasource not found with id: " + id));

        if ("CSV".equalsIgnoreCase(datasource.getType()) && datasource.getConfigJson() != null) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(datasource.getConfigJson());
                String filePath = root.has("filePath") ? root.get("filePath").asText(null) : null;
                String objectName = root.has("objectName") ? root.get("objectName").asText(null) : null;
                String bucket = root.has("bucket") ? root.get("bucket").asText(null) : null;

                minioStorageService.deleteCsvFile(filePath, objectName, bucket);
            } catch (Exception e) {
                log.warn("Failed to delete CSV files for datasource '{}': {}", datasource.getName(), e.getMessage());
            }
        }

        datasourceRepository.delete(datasource);
    }

    // ── Queries ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Datasource> getDatasourcesByStatus(DatasourceStatus status) {
        return datasourceRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Datasource> getDatasourcesByOwner(String owner) {
        return datasourceRepository.findByOwner(owner);
    }

    // ── Status management ────────────────────────────────────────────────

    @Override
    public Datasource activateDatasource(UUID id) {
        Datasource datasource = getDatasourceById(id);
        datasource.setStatus(DatasourceStatus.ACTIVE);
        return datasourceRepository.save(datasource);
    }

    @Override
    public Datasource disableDatasource(UUID id) {
        Datasource datasource = getDatasourceById(id);
        datasource.setStatus(DatasourceStatus.DISABLED);
        return datasourceRepository.save(datasource);
    }

    @Override
    public Datasource archiveDatasource(UUID id) {
        Datasource datasource = getDatasourceById(id);
        datasource.setStatus(DatasourceStatus.ARCHIVED);
        return datasourceRepository.save(datasource);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean isNameTaken(String name) {
        return datasourceRepository.existsByName(name);
    }

    // ── Configuration ────────────────────────────────────────────────────

    @Override
    public Datasource saveConfiguration(UUID id, String configJson) {
        Datasource datasource = getDatasourceById(id);
        datasource.setConfigJson(configJson);
        return datasourceRepository.save(datasource);
    }

    @Override
    @Transactional(readOnly = true)
    public String getConfiguration(UUID id) {
        Datasource datasource = getDatasourceById(id);
        return datasource.getConfigJson();
    }

    // ── Connection Testing ───────────────────────────────────────────────

    @Override
    public ConnectionTestResult testConnection(UUID id) {
        Datasource datasource = getDatasourceById(id);
        String configJson = datasource.getConfigJson();

        if (configJson == null || configJson.isBlank()) {
            return ConnectionTestResult.failure(
                    "No configuration saved for datasource '" + datasource.getName() + "'", 0);
        }

        ConnectorConfig config;
        try {
            config = parseConfig(datasource.getType(), configJson, datasource.getName());
        } catch (IllegalArgumentException e) {
            return ConnectionTestResult.failure(e.getMessage(), 0);
        }

        DataSourceConnector connector = connectorFactory.createConnector(config);
        return connector.testConnection();
    }

    // ── Dataset Discovery & Import ───────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DatasetDescriptor> discoverDatasets(UUID id) {
        Datasource datasource = getDatasourceById(id);
        String configJson = datasource.getConfigJson();

        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException(
                    "No configuration saved for datasource '" + datasource.getName() + "'");
        }

        ConnectorConfig config = parseConfig(datasource.getType(), configJson, datasource.getName());
        DataSourceConnector connector = connectorFactory.createConnector(config);
        return connector.discoverDatasets();
    }

    @Override
    public List<Dataset> importDatasets(UUID id, List<String> datasetIds) {
        Datasource datasource = getDatasourceById(id);
        if (datasetIds == null || datasetIds.isEmpty()) {
            return List.of();
        }

        List<DatasetDescriptor> descriptors = List.of();
        try {
            descriptors = discoverDatasets(id);
        } catch (Exception ignored) {
        }

        java.util.Map<String, DatasetDescriptor> descriptorMap = new java.util.HashMap<>();
        for (DatasetDescriptor d : descriptors) {
            if (d.name() != null)
                descriptorMap.put(d.name(), d);
            if (d.id() != null)
                descriptorMap.put(d.id(), d);
        }

        List<Dataset> imported = new ArrayList<>();
        for (String datasetName : datasetIds) {
            String trimmedName = datasetName.trim();
            if (trimmedName.isEmpty())
                continue;

            DatasetDescriptor descriptor = descriptorMap.get(trimmedName);
            Long rowCount = descriptor != null ? descriptor.rowCount() : null;

            Dataset dataset = datasetRepository.findByDatasourceIdAndName(id, trimmedName)
                    .orElseGet(() -> {
                        Dataset d = Dataset.builder()
                                .name(trimmedName)
                                .type(datasource.getType())
                                .status(DatasetStatus.ACTIVE)
                                .datasource(datasource)
                                .lastDiscovered(LocalDateTime.now())
                                .build();
                        datasource.getDatasets().add(d);
                        return d;
                    });

            dataset.setLastDiscovered(LocalDateTime.now());
            if (rowCount != null) {
                dataset.setRowCount(rowCount);
            }
            Dataset saved = datasetRepository.save(dataset);
            imported.add(saved);
        }

        return imported;
    }

    /**
     * Parses the JSON configuration string into the appropriate
     * {@link ConnectorConfig} subtype based on the datasource type.
     */
    private ConnectorConfig parseConfig(String type, String configJson, String datasourceName) {
        try {
            JsonNode root = objectMapper.readTree(configJson);

            return switch (type.toUpperCase()) {
                case "POSTGRESQL" -> {
                    JsonNode pg = root;
                    String host = getJsonText(pg, "host", "localhost");
                    int port = getJsonInt(pg, "port", 5432);
                    String database = getJsonText(pg, "database", "postgres");
                    String schema = getJsonText(pg, "schema", "public");
                    String username = getJsonText(pg, "username", "postgres");
                    String password = getJsonText(pg, "password", "");
                    boolean ssl = getJsonBoolean(pg, "ssl", false);
                    int connectionTimeoutMs = getJsonInt(pg, "connectionTimeoutMs", 30000);
                    int fetchSize = getJsonInt(pg, "fetchSize", 10000);

                    yield new ConnectorConfig.Postgres(
                            host, port, database, schema, username, password,
                            ssl, connectionTimeoutMs, fetchSize, datasourceName);
                }
                case "CSV" -> {
                    String filePath = getJsonText(root, "filePath", "");
                    if (filePath.isBlank()) {
                        throw new IllegalArgumentException(
                                "filePath is required for CSV datasource");
                    }
                    char delimiter = (char) getJsonInt(root, "delimiter", (int) ',');
                    boolean header = getJsonBoolean(root, "header", true);
                    String encoding = getJsonText(root, "encoding", "UTF-8");
                    char quoteChar = (char) getJsonInt(root, "quoteChar", (int) '"');
                    char escapeChar = (char) getJsonInt(root, "escapeChar", (int) '\\');
                    boolean inferSchema = getJsonBoolean(root, "inferSchema", true);

                    yield new ConnectorConfig.Csv(
                            filePath, delimiter, header, encoding, quoteChar,
                            escapeChar, inferSchema, datasourceName);
                }
                default -> throw new IllegalArgumentException(
                        "Unsupported datasource type: " + type);
            };
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Failed to parse configuration JSON: " + e.getMessage());
        }
    }

    private String getJsonText(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asText() : defaultValue;
    }

    private int getJsonInt(JsonNode node, String field, int defaultValue) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asInt(defaultValue) : defaultValue;
    }

    private boolean getJsonBoolean(JsonNode node, String field, boolean defaultValue) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asBoolean(defaultValue) : defaultValue;
    }
}
