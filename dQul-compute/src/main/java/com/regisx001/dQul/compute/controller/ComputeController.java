package com.regisx001.dQul.compute.controller;

import com.regisx001.dQul.compute.service.SparkComputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/compute")
@Tag(name = "Compute API", description = "Compute microservice health and status endpoints")
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
}
