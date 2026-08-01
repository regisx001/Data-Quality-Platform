package com.regisx001.dQul.dataset.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnDetailDto {

    private UUID id;
    private String name;
    private String dataType;
    private Boolean isNullable;
    private Boolean isPrimaryKey;

    // Latest Profile Stats (if available)
    private Long nullCount;
    private Double nullPercentage;
    private Long distinctCount;
    private String minValue;
    private String maxValue;
    private Double avgValue;
    private LocalDateTime profiledAt;
}
