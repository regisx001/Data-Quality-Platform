package com.regisx001.dQul.datasource.service;

import java.time.LocalDateTime;
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
import com.regisx001.dQul.datasource.domain.Datasource;
import com.regisx001.dQul.datasource.domain.DatasourceStatus;
import com.regisx001.dQul.datasource.repository.DatasourceRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class DatasourceServiceImpl implements DatasourceService {

    private final DatasourceRepository datasourceRepository;
    private final ConnectorFactory connectorFactory;
    private final ObjectMapper objectMapper;

    public DatasourceServiceImpl(DatasourceRepository datasourceRepository,
            ConnectorFactory connectorFactory, ObjectMapper objectMapper) {
        this.datasourceRepository = datasourceRepository;
        this.connectorFactory = connectorFactory;
        this.objectMapper = objectMapper;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    @Override
    public Datasource createDatasource(String name, String type, String description,
            String owner) {
        if (datasourceRepository.existsByName(name)) {
            throw new IllegalArgumentException(
                    "Datasource with name '" + name + "' already exists");
        }

        Datasource datasource = Datasource.builder()
                .name(name)
                .type(type)
                .description(description)
                .status(DatasourceStatus.REGISTERED)
                .owner(owner)
                .registrationDate(LocalDateTime.now())
                .build();

        return datasourceRepository.save(datasource);
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

    @Override
    public void deleteDatasource(UUID id) {
        if (!datasourceRepository.existsById(id)) {
            throw new EntityNotFoundException(
                    "Datasource not found with id: " + id);
        }
        datasourceRepository.deleteById(id);
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
