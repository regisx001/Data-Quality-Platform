package com.regisx001.dQul.dataset.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.regisx001.dQul.dataset.domain.DatasetStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetDetailResponse {

    private UUID id;
    private String name;
    private String s3aUri;
    private String description;
    private String type;
    private DatasetStatus status;
    private Long rowCount;
    private LocalDateTime lastDiscovered;
    private LocalDateTime lastValidated;
    private String domain;
    private String tags;

    // Datasource Summary
    private UUID datasourceId;
    private String datasourceName;
    private String datasourceType;

    // Columns with profiling metadata
    private List<ColumnDetailDto> columns;
}
