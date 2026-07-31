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
import com.regisx001.dQul.connector.ConnectorConfig;
import com.regisx001.dQul.connector.DataSourceConnector;
import com.regisx001.dQul.connector.api.ColumnMetadata;
import com.regisx001.dQul.connector.api.ConnectionTestResult;
import com.regisx001.dQul.connector.api.DataType;
import com.regisx001.dQul.connector.api.DatasetDescriptor;
import com.regisx001.dQul.connector.api.DatasetMetadata;
import com.regisx001.dQul.connector.api.DatasetType;

public class CsvDataSourceConnector implements DataSourceConnector {

    private static final Logger log = LoggerFactory.getLogger(CsvDataSourceConnector.class);

    private final ConnectorConfig.Csv config;
    private final Path filePath;
    private final SparkSessionProvider sparkSessionProvider;

    public CsvDataSourceConnector(ConnectorConfig.Csv config, SparkSessionProvider sparkSessionProvider) {
        this.config = config;
        this.sparkSessionProvider = sparkSessionProvider;
        this.filePath = Paths.get(config.filePath()).toAbsolutePath().normalize();
    }

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
        String fileName = filePath.getFileName().toString();
        String id = fileName;
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

    private long estimateRowCount() throws IOException {
        long totalBytes = Files.size(filePath);

        if (totalBytes < 10_000_000) {
            try (var lines = Files.lines(filePath)) {
                long count = lines.count();
                return config.header() ? Math.max(0, count - 1) : count;
            }
        }

        int sampleSize = 1_048_576;
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

        return config.header() ? Math.max(0, estimated - 1) : estimated;
    }

    private List<ColumnMetadata> inferColumns() {
        List<ColumnMetadata> columns = new ArrayList<>();
        Charset charset = Charset.forName(config.encoding());

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(filePath), charset))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                columns.add(new ColumnMetadata("column_0", DataType.STRING, true));
                return columns;
            }

            String[] headers = parseCsvLine(headerLine);
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

    private static DataType inferType(String[] fields, int index) {
        if (index >= fields.length) {
            return DataType.STRING;
        }
        String value = fields[index].strip();

        if (value.isEmpty() || value.equalsIgnoreCase("null")
                || value.equalsIgnoreCase("\\n")) {
            return DataType.STRING;
        }

        try {
            Long.parseLong(value);
            if (value.length() > 9) {
                return DataType.LONG;
            }
            return DataType.INTEGER;
        } catch (NumberFormatException ignored) {
        }

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
