package com.regisx001.dQul.dataset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnProfileResultDto {

    private String columnName;
    private String dataType;
    private Long nullCount;
    private Double nullPercentage;
    private Long distinctCount;
    private String minValue;
    private String maxValue;
    private Double avgValue;
    private LocalDateTime profiledAt;
}
