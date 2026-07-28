package com.regisx001.dQul.datasource;

import java.util.List;
import java.util.UUID;

import com.regisx001.dQul.datasource.Datasource;
import com.regisx001.dQul.datasource.DatasourceStatus;

public interface DatasourceService {

    // ── CRUD ──────────────────────────────────────────────────────────────

    Datasource createDatasource(String name, String type, String description,
            String owner);

    Datasource getDatasourceById(UUID id);

    Datasource getDatasourceByName(String name);

    List<Datasource> getAllDatasources();

    Datasource updateDatasource(UUID id, String name, String type,
            String description, DatasourceStatus status);

    void deleteDatasource(UUID id);

    // ── Queries ───────────────────────────────────────────────────────────

    List<Datasource> getDatasourcesByStatus(DatasourceStatus status);

    List<Datasource> getDatasourcesByOwner(String owner);

    // ── Status management ────────────────────────────────────────────────

    Datasource activateDatasource(UUID id);

    Datasource disableDatasource(UUID id);

    Datasource archiveDatasource(UUID id);

    // ── Helpers ───────────────────────────────────────────────────────────

    boolean isNameTaken(String name);
}
