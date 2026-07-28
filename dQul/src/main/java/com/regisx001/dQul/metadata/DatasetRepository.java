package com.regisx001.dQul.metadata;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regisx001.dQul.metadata.Dataset;
import com.regisx001.dQul.metadata.DatasetStatus;
import com.regisx001.dQul.datasource.Datasource;

public interface DatasetRepository extends JpaRepository<Dataset, UUID> {

    List<Dataset> findByDatasourceId(UUID datasourceId);

    List<Dataset> findByStatus(DatasetStatus status);

    List<Dataset> findByDomain(String domain);

    List<Dataset> findByCriticality(String criticality);

    List<Dataset> findByDatasourceIdAndStatus(UUID datasourceId, DatasetStatus status);
}
