package com.regisx001.dQul.connector.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.regisx001.dQul.compute.spark.SparkSessionProvider;
import com.regisx001.dQul.connector.api.ColumnMetadata;
import com.regisx001.dQul.connector.api.ConnectionTestResult;
import com.regisx001.dQul.connector.api.DataReader;
import com.regisx001.dQul.connector.api.DataSourceConnector;
import com.regisx001.dQul.connector.api.DataType;
import com.regisx001.dQul.connector.api.DatasetDescriptor;
import com.regisx001.dQul.connector.api.DatasetMetadata;
import com.regisx001.dQul.connector.api.CsvConnectorConfig;
import com.regisx001.dQul.connector.api.DatasetType;

/**
 * CSV connector that exposes two faces:
 *
 * <p>
 * <b>Metadata API</b> — Inspects the file system (filename, size, delimiter,
 * encoding, header, sample rows) for dataset discovery and schema inference.
 * This is the face consumed by the UI and metadata services.
 *
 * <p>
 * <b>Compute API</b> — Produces a Spark {@link DataReader} backed by the
 * Spark CSV data source with configurable delimiter, quoting, and encoding.
 * This is the face consumed by the Validation Engine and profiling pipelines.
 */
public class CsvDataSourceConnector implements DataSourceConnector {

    private static final Logger log = LoggerFactory.getLogger(CsvDataSourceConnector.class);

    private final CsvConnectorConfig config;
    private final Path filePath;
    private final SparkSessionProvider sparkSessionProvider;

    public CsvDataSourceConnector(CsvConnectorConfig config, SparkSessionProvider sparkSessionProvider) {
        this.config = config;
        this.sparkSessionProvider = sparkSessionProvider;
        this.filePath = Paths.get(config.filePath()).toAbsolutePath().normalize();
    }

    // ── MetadataApi / ComputeApi ────────────────────────────────────────

    @Override
    public ConnectionTestResult testConnection() {
        long start = System.currentTimeMillis();

        if (!Files.exists(filePath)) {
            long elapsed = System.currentTimeMillis() - start;
            return ConnectionTestResult.failure(
                    "CSV file not found: " + filePath.toAbsolutePath(), elapsed);
        }
        if (!Files.isReadable(filePath)) {
            long elapsed = System.currentTimeMillis() - start;
            return ConnectionTestResult.failure(
                    "CSV file is not readable: " + filePath.toAbsolutePath(), elapsed);
        }

        long elapsed = System.currentTimeMillis() - start;
        return ConnectionTestResult.success(
                "CSV file verified: %s (%d bytes)"
                        .formatted(filePath.getFileName(), filePath.toFile().length()),
                elapsed);
    }

    @Override
    public List<DatasetDescriptor> discoverDatasets() {
        // A CSV file exposes exactly one dataset: the file itself.
        String fileName = filePath.getFileName().toString();
        String id = fileName; // Use the file name as the stable identifier
        String stem = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf('.'))
                : fileName;

        return List.of(new DatasetDescriptor(id, stem, DatasetType.FILE,
                "CSV file: " + filePath.toAbsolutePath()));
    }

    @Override
    public DatasetMetadata getMetadata(String datasetId) {
        long estimatedRows = -1;

        try {
            estimatedRows = estimateRowCount();
        } catch (IOException e) {
            log.warn("Could not estimate row count for '{}': {}",
                    filePath, e.getMessage());
        }

        List<ColumnMetadata> columns = inferColumns();

        return new DatasetMetadata(config.datasourceName(), columns, estimatedRows);
    }

    @Override
    public DataReader createReader(String datasetId) {
        return () -> sparkSessionProvider.get().read()
                .format("csv")
                .option("sep", String.valueOf(config.delimiter()))
                .option("header", config.header())
                .option("encoding", config.encoding())
                .option("quote", String.valueOf(config.quoteChar()))
                .option("escape", String.valueOf(config.escapeChar()))
                .option("inferSchema", config.inferSchema())
                .option("path", filePath.toAbsolutePath().toString())
                .load();
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * Estimates the row count by counting lines. For large files this reads
     * only the first chunk and extrapolates; for smaller files it reads all lines.
     */
    private long estimateRowCount() throws IOException {
        long totalBytes = Files.size(filePath);

        // For small files read all lines
        if (totalBytes < 10_000_000) { // < 10 MB
            try (var lines = Files.lines(filePath)) {
                long count = lines.count();
                return config.header() ? Math.max(0, count - 1) : count;
            }
        }

        // For larger files sample the first 1 MB
        int sampleSize = 1_048_576; // 1 MB
        byte[] buffer = new byte[sampleSize];
        long lineCount = 0;

        try (var is = Files.newInputStream(filePath)) {
            int bytesRead = is.read(buffer);
            if (bytesRead <= 0) {
                return 0;
            }
            for (int i = 0; i < bytesRead; i++) {
                if (buffer[i] == '\n') {
                    lineCount++;
                }
            }
        }

        double ratio = (double) totalBytes / sampleSize;
        long estimated = (long) (lineCount * ratio);

        // Subtract the header line if present
        return config.header() ? Math.max(0, estimated - 1) : estimated;
    }

    /**
     * Infers column metadata by reading the header line and assigning
     * all columns {@link DataType#STRING} by default. When
     * {@code inferSchema} is enabled, Spark performs actual type inference
     * at read time; this method provides a conservative schema for
     * early metadata display.
     */
    private List<ColumnMetadata> inferColumns() {
        List<ColumnMetadata> columns = new ArrayList<>();
        Charset charset = Charset.forName(config.encoding());

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(filePath), charset))) {

            // Read the header line
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                columns.add(new ColumnMetadata("column_0", DataType.STRING, true));
                return columns;
            }

            String[] headers = parseCsvLine(headerLine);

            // Optionally read the first data line for type hints
            String firstDataLine = reader.readLine();

            for (int i = 0; i < headers.length; i++) {
                String colName = headers[i].isBlank()
                        ? "column_" + i
                        : headers[i].strip();

                DataType type = DataType.STRING;
                if (firstDataLine != null) {
                    type = inferType(parseCsvLine(firstDataLine), i);
                }

                columns.add(new ColumnMetadata(colName, type, true));
            }

        } catch (IOException e) {
            log.warn("Could not read CSV header from '{}': {}",
                    filePath, e.getMessage());
            columns.add(new ColumnMetadata("column_0", DataType.STRING, true));
        }

        return columns;
    }

    /**
     * Simple CSV line parser that respects the configured delimiter and quote
     * character.
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == config.quoteChar()) {
                inQuotes = !inQuotes;
            } else if (c == config.delimiter() && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());

        return fields.toArray(String[]::new);
    }

    /**
     * Best-effort type inference from a single data value.
     */
    private static DataType inferType(String[] fields, int index) {
        if (index >= fields.length) {
            return DataType.STRING;
        }
        String value = fields[index].strip();

        if (value.isEmpty() || value.equalsIgnoreCase("null")
                || value.equalsIgnoreCase("\\n")) {
            return DataType.STRING;
        }

        // Try boolean
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")
                || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("no")
                || value.equals("1") || value.equals("0")) {
            // Heuristic: if it looks like a number and can be parsed, prefer numeric
            // But 1/0 are ambiguous, so keep STRING for safety unless we have more data
        }

        // Try integer
        try {
            Long.parseLong(value);
            if (value.length() > 9) {
                return DataType.LONG;
            }
            return DataType.INTEGER;
        } catch (NumberFormatException ignored) {
        }

        // Try double / decimal
        if (value.contains(".") || value.contains(",")
                || value.contains("e") || value.contains("E")) {
            try {
                Double.parseDouble(value.replace(",", ""));
                return DataType.DOUBLE;
            } catch (NumberFormatException ignored) {
            }
        }

        return DataType.STRING;
    }
}
