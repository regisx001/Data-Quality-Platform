package com.regisx001.dQul.dataset.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain object representing the complete profile of a table/dataset,
 * including table metadata, total row count, and column profiles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableProfile {

    private UUID datasetId;
    private String tableName;
    private Long rowCount;

    @Builder.Default
    private List<ColumnProfile> columnProfiles = new ArrayList<>();

    private LocalDateTime profiledAt;
}
