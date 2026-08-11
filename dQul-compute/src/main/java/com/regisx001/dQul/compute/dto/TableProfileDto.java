package com.regisx001.dQul.compute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableProfileDto {

    private UUID profileId;
    private UUID datasetId;
    private String tableName;
    private Long rowCount;
    private Integer columnCount;

    @Builder.Default
    private List<ColumnProfileDto> columnProfiles = new ArrayList<>();

    @Builder.Default
    private LocalDateTime profiledAt = LocalDateTime.now();
}
