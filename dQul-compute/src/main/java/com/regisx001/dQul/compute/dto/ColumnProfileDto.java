package com.regisx001.dQul.compute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnProfileDto {

    private String columnName;
    private String dataType;

    @Builder.Default
    private Long nullCount = 0L;

    @Builder.Default
    private Double nullPercentage = 0.0;

    @Builder.Default
    private Long distinctCount = 0L;

    private String minValue;
    private String maxValue;
    private Double avgValue;

    @Builder.Default
    private LocalDateTime profiledAt = LocalDateTime.now();
}
