package com.regisx001.dQul.datasource.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regisx001.dQul.datasource.domain.Datasource;
import com.regisx001.dQul.datasource.domain.DatasourceStatus;
import com.regisx001.dQul.datasource.repository.DatasourceRepository;
import com.regisx001.dQul.datasource.service.DatasourceService;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class DatasourceServiceImpl implements DatasourceService {

    private final DatasourceRepository datasourceRepository;

    public DatasourceServiceImpl(DatasourceRepository datasourceRepository) {
        this.datasourceRepository = datasourceRepository;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    @Override
    public Datasource createDatasource(String name, String type, String description,
            String owner) {
        if (datasourceRepository.existsByName(name)) {
            throw new IllegalArgumentException(
                    "Datasource with name '" + name + "' already exists");
        }

        Datasource datasource = Datasource.builder()
                .name(name)
                .type(type)
                .description(description)
                .status(DatasourceStatus.REGISTERED)
                .owner(owner)
                .registrationDate(LocalDateTime.now())
                .build();

        return datasourceRepository.save(datasource);
    }

    @Override
    @Transactional(readOnly = true)
    public Datasource getDatasourceById(UUID id) {
        return datasourceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Datasource not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Datasource getDatasourceByName(String name) {
        return datasourceRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Datasource not found with name: " + name));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Datasource> getAllDatasources() {
        return datasourceRepository.findAll();
    }

    @Override
    public Datasource updateDatasource(UUID id, String name, String type,
            String description, DatasourceStatus status) {
        Datasource datasource = getDatasourceById(id);

        if (name != null && !name.equals(datasource.getName())) {
            if (datasourceRepository.existsByName(name)) {
                throw new IllegalArgumentException(
                        "Datasource with name '" + name + "' already exists");
            }
            datasource.setName(name);
        }
        if (type != null) {
            datasource.setType(type);
        }
        if (description != null) {
            datasource.setDescription(description);
        }
        if (status != null) {
            datasource.setStatus(status);
        }

        return datasourceRepository.save(datasource);
    }

    @Override
    public void deleteDatasource(UUID id) {
        if (!datasourceRepository.existsById(id)) {
            throw new EntityNotFoundException(
                    "Datasource not found with id: " + id);
        }
        datasourceRepository.deleteById(id);
    }

    // ── Queries ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Datasource> getDatasourcesByStatus(DatasourceStatus status) {
        return datasourceRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Datasource> getDatasourcesByOwner(String owner) {
        return datasourceRepository.findByOwner(owner);
    }

    // ── Status management ────────────────────────────────────────────────

    @Override
    public Datasource activateDatasource(UUID id) {
        Datasource datasource = getDatasourceById(id);
        datasource.setStatus(DatasourceStatus.ACTIVE);
        return datasourceRepository.save(datasource);
    }

    @Override
    public Datasource disableDatasource(UUID id) {
        Datasource datasource = getDatasourceById(id);
        datasource.setStatus(DatasourceStatus.DISABLED);
        return datasourceRepository.save(datasource);
    }

    @Override
    public Datasource archiveDatasource(UUID id) {
        Datasource datasource = getDatasourceById(id);
        datasource.setStatus(DatasourceStatus.ARCHIVED);
        return datasourceRepository.save(datasource);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean isNameTaken(String name) {
        return datasourceRepository.existsByName(name);
    }
}
