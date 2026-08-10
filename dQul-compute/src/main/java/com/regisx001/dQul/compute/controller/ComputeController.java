package com.regisx001.dQul.compute.controller;

import com.regisx001.dQul.compute.dto.ComputeJobRequest;
import com.regisx001.dQul.compute.dto.ComputeJobResult;
import com.regisx001.dQul.compute.service.SparkComputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/compute")
@Tag(name = "Compute API", description = "Endpoints for Spark dataset processing and compute jobs")
public class ComputeController {

    private final SparkComputeService computeService;

    public ComputeController(SparkComputeService computeService) {
        this.computeService = computeService;
    }

    @GetMapping("/health")
    @Operation(summary = "Check Compute service and Spark status")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(Map.of(
                "service", "dQul-compute",
                "sparkActive", computeService.isSparkActive(),
                "sparkVersion", computeService.getSparkVersion()
        ));
    }

    @PostMapping("/jobs")
    @Operation(summary = "Execute a Spark compute job")
    public ResponseEntity<ComputeJobResult> submitJob(@RequestBody ComputeJobRequest request) {
        ComputeJobResult result = computeService.executeJob(request);
        return ResponseEntity.ok(result);
    }
}
