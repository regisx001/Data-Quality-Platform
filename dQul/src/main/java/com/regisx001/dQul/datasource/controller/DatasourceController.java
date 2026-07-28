package com.regisx001.dQul.datasource.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regisx001.dQul.common.responses.ApiErrorResponse;
import com.regisx001.dQul.datasource.domain.Datasource;
import com.regisx001.dQul.datasource.domain.DatasourceStatus;
import com.regisx001.dQul.datasource.service.DatasourceService;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/v1/datasources")
public class DatasourceController {

    private final DatasourceService datasourceService;

    public DatasourceController(DatasourceService datasourceService) {
        this.datasourceService = datasourceService;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> createDatasource(@RequestBody CreateDatasourceRequest request) {
        try {
            Datasource datasource = datasourceService.createDatasource(
                    request.name(),
                    request.type(),
                    request.description(),
                    request.owner());
            return ResponseEntity.status(HttpStatus.CREATED).body(datasource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping
    public ResponseEntity<List<Datasource>> getAllDatasources() {
        return ResponseEntity.ok(datasourceService.getAllDatasources());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDatasourceById(@PathVariable UUID id) {
        try {
            Datasource datasource = datasourceService.getDatasourceById(id);
            return ResponseEntity.ok(datasource);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<?> getDatasourceByName(@PathVariable String name) {
        try {
            Datasource datasource = datasourceService.getDatasourceByName(name);
            return ResponseEntity.ok(datasource);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDatasource(@PathVariable UUID id,
            @RequestBody UpdateDatasourceRequest request) {
        try {
            Datasource updated = datasourceService.updateDatasource(
                    id, request.name(), request.type(),
                    request.description(), request.status());
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDatasource(@PathVariable UUID id) {
        try {
            datasourceService.deleteDatasource(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────

    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<Datasource>> getDatasourcesByStatus(
            @PathVariable DatasourceStatus status) {
        return ResponseEntity.ok(
                datasourceService.getDatasourcesByStatus(status));
    }

    @GetMapping("/by-owner/{owner}")
    public ResponseEntity<List<Datasource>> getDatasourcesByOwner(
            @PathVariable String owner) {
        return ResponseEntity.ok(
                datasourceService.getDatasourcesByOwner(owner));
    }

    // ── Status management ────────────────────────────────────────────────

    @PatchMapping("/{id}/activate")
    public ResponseEntity<?> activateDatasource(@PathVariable UUID id) {
        try {
            Datasource datasource = datasourceService.activateDatasource(id);
            return ResponseEntity.ok(datasource);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<?> disableDatasource(@PathVariable UUID id) {
        try {
            Datasource datasource = datasourceService.disableDatasource(id);
            return ResponseEntity.ok(datasource);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<?> archiveDatasource(@PathVariable UUID id) {
        try {
            Datasource datasource = datasourceService.archiveDatasource(id);
            return ResponseEntity.ok(datasource);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }

    // ── Inner DTOs ───────────────────────────────────────────────────────

    public record CreateDatasourceRequest(
            String name,
            String type,
            String description,
            String owner) {
    }

    public record UpdateDatasourceRequest(
            String name,
            String type,
            String description,
            DatasourceStatus status) {
    }
}
