package com.regisx001.dQul.dataset.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regisx001.dQul.connector.ConnectorConfig;
import com.regisx001.dQul.connector.ConnectorFactory;
import com.regisx001.dQul.connector.DataSourceConnector;
import com.regisx001.dQul.connector.api.ColumnMetadata;
import com.regisx001.dQul.connector.api.DatasetMetadata;
import com.regisx001.dQul.dataset.domain.ColumnProfile;
import com.regisx001.dQul.dataset.domain.Dataset;
import com.regisx001.dQul.dataset.domain.DatasetColumn;
import com.regisx001.dQul.dataset.dto.ColumnDetailDto;
import com.regisx001.dQul.dataset.dto.DataPreviewResult;
import com.regisx001.dQul.dataset.dto.DatasetDetailResponse;
import com.regisx001.dQul.dataset.repository.ColumnProfileRepository;
import com.regisx001.dQul.dataset.repository.DatasetColumnRepository;
import com.regisx001.dQul.dataset.repository.DatasetRepository;
import com.regisx001.dQul.datasource.domain.Datasource;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class DatasetServiceImpl implements DatasetService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatasetServiceImpl.class);

    private final DatasetRepository datasetRepository;
    private final DatasetColumnRepository columnRepository;
    private final ColumnProfileRepository profileRepository;
    private final ConnectorFactory connectorFactory;
    private final ObjectMapper objectMapper;

    public DatasetServiceImpl(
            DatasetRepository datasetRepository,
            DatasetColumnRepository columnRepository,
            ColumnProfileRepository profileRepository,
            ConnectorFactory connectorFactory,
            ObjectMapper objectMapper) {
        this.datasetRepository = datasetRepository;
        this.columnRepository = columnRepository;
        this.profileRepository = profileRepository;
        this.connectorFactory = connectorFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public DatasetDetailResponse getDatasetById(UUID id) {
        Dataset dataset = resolveDataset(id);

        // Auto-extract columns if not populated yet
        if (dataset.getColumns() == null || dataset.getColumns().isEmpty()) {
            syncColumnsFromConnector(dataset);
        }

        return mapToDetailResponse(dataset);
    }

    @Override
    public DataPreviewResult getDatasetPreview(UUID id, int limit) {
        Dataset dataset = resolveDataset(id);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        Datasource datasource = dataset.getDatasource();

        if (datasource.getConfigJson() != null && !datasource.getConfigJson().isBlank()) {
            try {
                ConnectorConfig config = parseConfig(datasource.getType(), datasource.getConfigJson(),
                        datasource.getName());
                if (config instanceof ConnectorConfig.Postgres pgConfig) {
                    return fetchPostgresPreview(pgConfig, dataset.getName(), safeLimit);
                } else if (config instanceof ConnectorConfig.Csv csvConfig) {
                    return fetchCsvPreview(csvConfig, safeLimit);
                }
            } catch (Throwable ignored) {
            }
        }

        // Fallback demo/preview generation if connector is offline
        List<DatasetColumn> cols = dataset.getColumns();
        List<String> colNames = cols.stream().map(DatasetColumn::getName).toList();
        if (colNames.isEmpty()) {
            colNames = List.of("id", "name", "created_at");
        }

        List<Map<String, Object>> mockRows = new ArrayList<>();
        for (int r = 1; r <= safeLimit; r++) {
            Map<String, Object> mockRow = new HashMap<>();
            for (String col : colNames) {
                if (col.toLowerCase().contains("id")) {
                    mockRow.put(col, r);
                } else if (col.toLowerCase().contains("date") || col.toLowerCase().contains("time")
                        || col.toLowerCase().contains("at")) {
                    mockRow.put(col, LocalDateTime.now().minusDays(r).toString());
                } else if (col.toLowerCase().contains("price") || col.toLowerCase().contains("amount")
                        || col.toLowerCase().contains("count")) {
                    mockRow.put(col, Math.round(r * 15.5 * 100.0) / 100.0);
                } else {
                    mockRow.put(col, "Sample " + col + " #" + r);
                }
            }
            mockRows.add(mockRow);
        }

        return DataPreviewResult.builder()
                .columns(colNames)
                .rows(mockRows)
                .totalRows(mockRows.size())
                .build();
    }

    private DataPreviewResult fetchPostgresPreview(ConnectorConfig.Postgres config, String datasetName, int limit)
            throws Exception {
        String[] parts = datasetName.split("\\.", 2);
        String schema = (parts.length > 1 && !parts[0].isBlank()) ? parts[0]
                : (config.schema() != null && !config.schema().isBlank() ? config.schema() : "public");
        String tableName = parts.length > 1 ? parts[1] : datasetName;

        Properties props = new Properties();
        props.setProperty("user", config.username());
        props.setProperty("password", config.password());
        if (config.ssl()) {
            props.setProperty("ssl", "true");
            props.setProperty("sslmode", "require");
        }
        String url = config.jdbcUrl();

        String sql = "SELECT * FROM \"%s\".\"%s\" LIMIT ?".formatted(schema, tableName);

        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, props);
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                java.sql.ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                for (int i = 1; i <= colCount; i++) {
                    columns.add(meta.getColumnLabel(i));
                }

                while (rs.next()) {
                    Map<String, Object> rowMap = new HashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        Object val = rs.getObject(i);
                        rowMap.put(meta.getColumnLabel(i), val != null ? val.toString() : null);
                    }
                    rows.add(rowMap);
                }
            }
        }

        return DataPreviewResult.builder()
                .columns(columns)
                .rows(rows)
                .totalRows(rows.size())
                .build();
    }

    private DataPreviewResult fetchCsvPreview(ConnectorConfig.Csv config, int limit) throws Exception {
        java.nio.file.Path path = java.nio.file.Paths.get(config.filePath()).toAbsolutePath().normalize();
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        if (java.nio.file.Files.exists(path)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    java.nio.file.Files.newInputStream(path), java.nio.charset.Charset.forName(config.encoding())))) {
                String headerLine = reader.readLine();
                if (headerLine != null) {
                    String[] headers = headerLine.split(String.valueOf(config.delimiter()));
                    for (String h : headers) {
                        columns.add(h.trim().replace("\"", ""));
                    }

                    String line;
                    int count = 0;
                    while ((line = reader.readLine()) != null && count < limit) {
                        String[] values = line.split(String.valueOf(config.delimiter()));
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 0; i < columns.size(); i++) {
                            String colName = columns.get(i);
                            String val = i < values.length ? values[i].trim().replace("\"", "") : null;
                            row.put(colName, val);
                        }
                        rows.add(row);
                        count++;
                    }
                }
            }
        }

        return DataPreviewResult.builder()
                .columns(columns)
                .rows(rows)
                .totalRows(rows.size())
                .build();
    }

    public static boolean isNullValue(Object val) {
        if (val == null) {
            return true;
        }
        String s = val.toString().trim();
        return s.isEmpty() || s.equalsIgnoreCase("null") || s.equalsIgnoreCase("none") || s.equalsIgnoreCase("n/a");
    }

    public static boolean isMinMaxComputable(String dataType) {
        if (dataType == null) return false;
        String type = dataType.toUpperCase().trim();

        // Numeric types
        boolean isNumeric = type.contains("INT") || type.contains("NUM") || type.contains("FLOAT")
                || type.contains("DOUBLE") || type.contains("DECIMAL") || type.contains("REAL")
                || type.contains("SERIAL") || type.contains("LONG") || type.contains("SHORT")
                || type.contains("BYTE") || type.contains("NUMBER");

        // Date and Time types
        boolean isDateTime = type.contains("DATE") || type.contains("TIME") || type.contains("TIMESTAMP");

        return isNumeric || isDateTime;
    }

    @Override
    @Transactional
    public DatasetDetailResponse profileDataset(UUID id) {
        Dataset dataset = resolveDataset(id);
        profileDatasetInternal(dataset);
        return mapToDetailResponse(dataset);
    }

    @Override
    @Transactional
    public void deleteDataset(UUID id) {
        Dataset dataset = resolveDataset(id);
        datasetRepository.delete(dataset);
    }

    private void profileDatasetInternal(Dataset dataset) {
        if (dataset.getColumns() == null || dataset.getColumns().isEmpty()) {
            syncColumnsFromConnector(dataset);
        }

        LocalDateTime now = LocalDateTime.now();
        dataset.setLastValidated(now);

        Datasource datasource = dataset.getDatasource();
        boolean profiledReal = false;

        if (datasource != null && datasource.getConfigJson() != null && !datasource.getConfigJson().isBlank()) {
            try {
                ConnectorConfig config = parseConfig(datasource.getType(), datasource.getConfigJson(),
                        datasource.getName());
                if (config instanceof ConnectorConfig.Postgres pgConfig) {
                    profilePostgresDataset(dataset, pgConfig, now);
                    profiledReal = true;
                } else if (config instanceof ConnectorConfig.Csv csvConfig) {
                    profileCsvDataset(dataset, csvConfig, now);
                    profiledReal = true;
                }
            } catch (Exception e) {
                log.warn("Real database profiling failed for dataset '{}', falling back to default profiling: {}",
                        dataset.getName(), e.getMessage());
            }
        }

        if (!profiledReal) {
            profileFallbackDataset(dataset, now);
        }

        datasetRepository.save(dataset);
    }

    private void profilePostgresDataset(Dataset dataset, ConnectorConfig.Postgres config, LocalDateTime now)
            throws Exception {
        String[] parts = dataset.getName().split("\\.", 2);
        String schema = (parts.length > 1 && !parts[0].isBlank()) ? parts[0]
                : (config.schema() != null && !config.schema().isBlank() ? config.schema() : "public");
        String tableName = parts.length > 1 ? parts[1] : dataset.getName();

        Properties props = new Properties();
        props.setProperty("user", config.username());
        props.setProperty("password", config.password());
        if (config.ssl()) {
            props.setProperty("ssl", "true");
            props.setProperty("sslmode", "require");
        }
        String url = config.jdbcUrl();

        try (Connection conn = DriverManager.getConnection(url, props)) {
            long totalRows = 0L;
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt
                            .executeQuery("SELECT COUNT(*) FROM \"%s\".\"%s\"".formatted(schema, tableName))) {
                if (rs.next()) {
                    totalRows = rs.getLong(1);
                }
            }
            if (totalRows <= 0)
                totalRows = 1L;
            dataset.setRowCount(totalRows);

            for (DatasetColumn col : dataset.getColumns()) {
                String colName = col.getName();
                boolean isNumeric = col.getDataType() != null && (col.getDataType().toUpperCase().contains("INT") ||
                        col.getDataType().toUpperCase().contains("NUM") ||
                        col.getDataType().toUpperCase().contains("FLOAT") ||
                        col.getDataType().toUpperCase().contains("DOUBLE") ||
                        col.getDataType().toUpperCase().contains("DECIMAL") ||
                        col.getDataType().toUpperCase().contains("REAL") ||
                        col.getDataType().toUpperCase().contains("SERIAL"));

                boolean isComputable = isMinMaxComputable(col.getDataType());

                String minSql = isComputable
                        ? "MIN(CASE WHEN \"%1$s\" IS NOT NULL AND LOWER(TRIM(\"%1$s\"::text)) <> 'null' AND TRIM(\"%1$s\"::text) <> '' THEN \"%1$s\" END)::text AS min_val".formatted(colName)
                        : "NULL::text AS min_val";

                String maxSql = isComputable
                        ? "MAX(CASE WHEN \"%1$s\" IS NOT NULL AND LOWER(TRIM(\"%1$s\"::text)) <> 'null' AND TRIM(\"%1$s\"::text) <> '' THEN \"%1$s\" END)::text AS max_val".formatted(colName)
                        : "NULL::text AS max_val";

                String avgSql = isNumeric
                        ? "AVG(CASE WHEN \"%1$s\" IS NOT NULL AND LOWER(TRIM(\"%1$s\"::text)) <> 'null' AND TRIM(\"%1$s\"::text) <> '' THEN \"%1$s\" END) AS avg_val".formatted(colName)
                        : "NULL::numeric AS avg_val";

                String colSql = """
                        SELECT
                            COUNT(CASE WHEN "%1$s" IS NULL OR LOWER(TRIM("%1$s"::text)) = 'null' OR TRIM("%1$s"::text) = '' OR LOWER(TRIM("%1$s"::text)) = 'none' OR LOWER(TRIM("%1$s"::text)) = 'n/a' THEN 1 END) AS null_count,
                            COUNT(DISTINCT CASE WHEN "%1$s" IS NOT NULL AND LOWER(TRIM("%1$s"::text)) <> 'null' AND TRIM("%1$s"::text) <> '' AND LOWER(TRIM("%1$s"::text)) <> 'none' AND LOWER(TRIM("%1$s"::text)) <> 'n/a' THEN "%1$s"::text END) AS distinct_count,
                            %2$s,
                            %3$s,
                            %4$s
                        FROM "%5$s"."%6$s"
                        """
                        .formatted(colName, minSql, maxSql, avgSql, schema, tableName);

                long nullCount = 0L;
                long distinctCount = 0L;
                String minVal = null;
                String maxVal = null;
                Double avgVal = null;

                try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(colSql)) {
                    if (rs.next()) {
                        nullCount = rs.getLong("null_count");
                        distinctCount = rs.getLong("distinct_count");
                        minVal = rs.getString("min_val");
                        maxVal = rs.getString("max_val");
                        Object avgObj = rs.getObject("avg_val");
                        if (avgObj != null) {
                            avgVal = ((Number) avgObj).doubleValue();
                        }
                    }
                } catch (Exception colEx) {
                    log.warn("Failed to profile column '{}' in Postgres, using default column profile: {}", colName,
                            colEx.getMessage());
                    nullCount = col.isNullable() ? 1L : 0L;
                    distinctCount = 1L;
                }

                double nullPct = Math.round(((double) nullCount / totalRows) * 10000.0) / 100.0;

                saveOrUpdateProfile(col, nullCount, nullPct, distinctCount, minVal, maxVal, avgVal, now);
            }
        }
    }

    private ColumnProfile saveOrUpdateProfile(DatasetColumn col, long nullCount, double nullPct,
            long distinctCount, String minVal, String maxVal, Double avgVal, LocalDateTime profiledAt) {
        ColumnProfile profile = (col.getId() != null)
                ? profileRepository.findFirstByColumnIdOrderByProfiledAtDesc(col.getId())
                        .orElseGet(() -> ColumnProfile.builder().column(col).build())
                : ColumnProfile.builder().column(col).build();

        profile.setNullCount(nullCount);
        profile.setNullPercentage(nullPct);
        profile.setDistinctCount(distinctCount);
        profile.setMinValue(minVal);
        profile.setMaxValue(maxVal);
        profile.setAvgValue(avgVal);
        profile.setProfiledAt(profiledAt);

        return profileRepository.save(profile);
    }

    private void profileCsvDataset(Dataset dataset, ConnectorConfig.Csv config, LocalDateTime now) throws Exception {
        java.nio.file.Path path = java.nio.file.Paths.get(config.filePath()).toAbsolutePath().normalize();
        if (!java.nio.file.Files.exists(path)) {
            profileFallbackDataset(dataset, now);
            return;
        }

        List<String> headers = new ArrayList<>();
        List<String[]> dataRows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(java.nio.file.Files.newInputStream(path),
                java.nio.charset.Charset.forName(config.encoding())))) {
            String headerLine = reader.readLine();
            if (headerLine != null) {
                for (String h : headerLine.split(String.valueOf(config.delimiter()))) {
                    headers.add(h.trim().replace("\"", ""));
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    dataRows.add(line.split(String.valueOf(config.delimiter())));
                }
            }
        }

        long totalRows = Math.max(1L, dataRows.size());
        dataset.setRowCount(totalRows);

        for (DatasetColumn col : dataset.getColumns()) {
            int colIdx = headers.indexOf(col.getName());
            long nullCount = 0L;
            java.util.Set<String> distinctSet = new java.util.HashSet<>();
            String minVal = null;
            String maxVal = null;
            Double minNum = null;
            Double maxNum = null;
            double sum = 0;
            long numCount = 0;
            boolean isComputable = isMinMaxComputable(col.getDataType());

            for (String[] row : dataRows) {
                String val = (colIdx >= 0 && colIdx < row.length) ? row[colIdx].trim().replace("\"", "") : null;
                if (isNullValue(val)) {
                    nullCount++;
                } else {
                    distinctSet.add(val);
                    if (isComputable) {
                        try {
                            double d = Double.parseDouble(val);
                            sum += d;
                            numCount++;
                            if (minNum == null || d < minNum) {
                                minNum = d;
                            }
                            if (maxNum == null || d > maxNum) {
                                maxNum = d;
                            }
                        } catch (NumberFormatException ignored) {
                            if (minVal == null || val.compareTo(minVal) < 0)
                                minVal = val;
                            if (maxVal == null || val.compareTo(maxVal) > 0)
                                maxVal = val;
                        }
                    }
                }
            }

            if (isComputable) {
                if (numCount > 0) {
                    if (minNum != null) {
                        minVal = (minNum % 1 == 0) ? String.valueOf(minNum.longValue()) : String.valueOf(minNum);
                    }
                    if (maxNum != null) {
                        maxVal = (maxNum % 1 == 0) ? String.valueOf(maxNum.longValue()) : String.valueOf(maxNum);
                    }
                }
            } else {
                minVal = null;
                maxVal = null;
            }

            double nullPct = Math.round(((double) nullCount / totalRows) * 10000.0) / 100.0;
            Double avgVal = numCount > 0 ? (sum / numCount) : null;

            saveOrUpdateProfile(col, nullCount, nullPct, (long) distinctSet.size(), minVal, maxVal, avgVal, now);
        }
    }

    private void profileFallbackDataset(Dataset dataset, LocalDateTime now) {
        for (DatasetColumn col : dataset.getColumns()) {
            long totalRows = dataset.getRowCount() != null && dataset.getRowCount() > 0 ? dataset.getRowCount() : 100L;
            long nullCount = col.isNullable() ? (long) (Math.random() * (totalRows * 0.05)) : 0L;
            double nullPct = Math.round(((double) nullCount / totalRows) * 10000.0) / 100.0;
            long distinctCount = Math.max(1L, (long) (totalRows * (0.1 + Math.random() * 0.8)));

            boolean isComputable = isMinMaxComputable(col.getDataType());
            boolean isNumeric = col.getDataType() != null && (col.getDataType().toUpperCase().contains("INT")
                    || col.getDataType().toUpperCase().contains("NUM")
                    || col.getDataType().toUpperCase().contains("FLOAT")
                    || col.getDataType().toUpperCase().contains("DOUBLE"));

            String minVal = isComputable ? (isNumeric ? "1" : "2026-01-01") : null;
            String maxVal = isComputable ? (isNumeric ? String.valueOf(totalRows) : "2026-12-31") : null;
            Double avgVal = isNumeric ? (totalRows / 2.0) : null;

            saveOrUpdateProfile(col, nullCount, nullPct, distinctCount, minVal, maxVal, avgVal, now);
        }
    }

    private void syncColumnsFromConnector(Dataset dataset) {
        Datasource datasource = dataset.getDatasource();
        if (datasource.getConfigJson() == null || datasource.getConfigJson().isBlank()) {
            return;
        }

        try {
            ConnectorConfig config = parseConfig(datasource.getType(), datasource.getConfigJson(),
                    datasource.getName());
            DataSourceConnector connector = connectorFactory.createConnector(config);
            DatasetMetadata metadata = connector.getMetadata(dataset.getName());

            if (metadata != null && metadata.columns() != null) {
                for (ColumnMetadata colMeta : metadata.columns()) {
                    Optional<DatasetColumn> existing = columnRepository.findByDatasetIdAndName(dataset.getId(),
                            colMeta.name());
                    if (existing.isEmpty()) {
                        DatasetColumn col = DatasetColumn.builder()
                                .dataset(dataset)
                                .name(colMeta.name())
                                .dataType(colMeta.type() != null ? colMeta.type().name() : "VARCHAR")
                                .isNullable(colMeta.nullable())
                                .isPrimaryKey(colMeta.name().toLowerCase().contains("id"))
                                .build();
                        DatasetColumn savedCol = columnRepository.save(col);
                        dataset.getColumns().add(savedCol);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private DatasetDetailResponse mapToDetailResponse(Dataset dataset) {
        List<ColumnDetailDto> colDtos = new ArrayList<>();
        if (dataset.getColumns() != null) {
            for (DatasetColumn col : dataset.getColumns()) {
                Optional<ColumnProfile> latestProfile = col.getId() != null
                        ? profileRepository.findFirstByColumnIdOrderByProfiledAtDesc(col.getId())
                        : Optional.empty();

                colDtos.add(ColumnDetailDto.builder()
                        .id(col.getId())
                        .name(col.getName())
                        .dataType(col.getDataType())
                        .isNullable(col.isNullable())
                        .isPrimaryKey(col.isPrimaryKey())
                        .nullCount(latestProfile.map(ColumnProfile::getNullCount).orElse(0L))
                        .nullPercentage(latestProfile.map(ColumnProfile::getNullPercentage).orElse(0.0))
                        .distinctCount(latestProfile.map(ColumnProfile::getDistinctCount).orElse(0L))
                        .minValue(latestProfile.map(ColumnProfile::getMinValue).orElse(null))
                        .maxValue(latestProfile.map(ColumnProfile::getMaxValue).orElse(null))
                        .avgValue(latestProfile.map(ColumnProfile::getAvgValue).orElse(null))
                        .profiledAt(latestProfile.map(ColumnProfile::getProfiledAt).orElse(null))
                        .build());
            }
        }

        return DatasetDetailResponse.builder()
                .id(dataset.getId())
                .name(dataset.getName())
                .description(dataset.getDescription())
                .type(dataset.getType())
                .status(dataset.getStatus() != null ? dataset.getStatus()
                        : com.regisx001.dQul.dataset.domain.DatasetStatus.ACTIVE)
                .rowCount(dataset.getRowCount())
                .lastDiscovered(dataset.getLastDiscovered())
                .lastValidated(dataset.getLastValidated())
                .domain(dataset.getDomain())
                .tags(dataset.getTags())
                .datasourceId(dataset.getDatasource().getId())
                .datasourceName(dataset.getDatasource().getName())
                .datasourceType(dataset.getDatasource().getType())
                .columns(colDtos)
                .build();
    }

    private ConnectorConfig parseConfig(String type, String configJson, String datasourceName) throws Exception {
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(configJson);
        return switch (type.toUpperCase()) {
            case "POSTGRESQL" -> new ConnectorConfig.Postgres(
                    root.path("host").asText("localhost"),
                    root.path("port").asInt(5432),
                    root.path("database").asText("postgres"),
                    root.path("schema").asText("public"),
                    root.path("username").asText("postgres"),
                    root.path("password").asText(""),
                    root.path("ssl").asBoolean(false),
                    30000, 10000, datasourceName);
            case "CSV" -> new ConnectorConfig.Csv(
                    root.path("filePath").asText(""),
                    ',', true, "UTF-8", '"', '\\', true, datasourceName);
            default -> throw new IllegalArgumentException("Unsupported datasource type: " + type);
        };
    }

    private Dataset resolveDataset(UUID id) {
        return datasetRepository.findById(id)
                .or(() -> datasetRepository.findByName(id.toString()))
                .orElseThrow(() -> new EntityNotFoundException("Dataset not found with id: " + id));
    }
}
