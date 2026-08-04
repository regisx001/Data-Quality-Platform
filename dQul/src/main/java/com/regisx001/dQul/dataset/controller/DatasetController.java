package com.regisx001.dQul.dataset.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regisx001.dQul.dataset.dto.DataPreviewResult;
import com.regisx001.dQul.dataset.dto.DatasetDetailResponse;
import com.regisx001.dQul.dataset.service.DatasetService;

@RestController
@RequestMapping("/api/v1/datasets")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DatasetDetailResponse> getDatasetById(@PathVariable UUID id) {
        DatasetDetailResponse response = datasetService.getDatasetById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<DataPreviewResult> getDatasetPreview(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "50") int limit) {
        DataPreviewResult response = datasetService.getDatasetPreview(id, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/profile")
    public ResponseEntity<DatasetDetailResponse> profileDataset(@PathVariable UUID id) {
        DatasetDetailResponse response = datasetService.profileDataset(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDataset(@PathVariable UUID id) {
        datasetService.deleteDataset(id);
        return ResponseEntity.noContent().build();
    }
}
