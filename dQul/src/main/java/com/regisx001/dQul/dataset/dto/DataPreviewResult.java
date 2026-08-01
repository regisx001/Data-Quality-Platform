package com.regisx001.dQul.dataset.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataPreviewResult {

    private List<String> columns;
    private List<Map<String, Object>> rows;
    private int totalRows;
}
