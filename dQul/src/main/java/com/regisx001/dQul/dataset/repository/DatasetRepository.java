package com.regisx001.dQul.dataset.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regisx001.dQul.dataset.domain.Dataset;
import com.regisx001.dQul.dataset.domain.DatasetStatus;
import com.regisx001.dQul.datasource.domain.Datasource;

public interface DatasetRepository extends JpaRepository<Dataset, UUID> {

    List<Dataset> findByDatasourceId(UUID datasourceId);

    List<Dataset> findByStatus(DatasetStatus status);

    List<Dataset> findByDomain(String domain);

    List<Dataset> findByCriticality(String criticality);

    List<Dataset> findByDatasourceIdAndStatus(UUID datasourceId, DatasetStatus status);

    boolean existsByDatasourceIdAndName(UUID datasourceId, String name);

    Optional<Dataset> findByDatasourceIdAndName(UUID datasourceId, String name);
}
