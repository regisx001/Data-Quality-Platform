package com.regisx001.dQul.dataset.dto;

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
public class TableProfileResultDto {

    private UUID profileId;
    private UUID datasetId;
    private String tableName;
    private Long rowCount;
    private Integer columnCount;

    @Builder.Default
    private List<ColumnProfileResultDto> columnProfiles = new ArrayList<>();

    private LocalDateTime profiledAt;
}
