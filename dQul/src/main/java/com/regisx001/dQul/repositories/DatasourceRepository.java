package com.regisx001.dQul.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regisx001.dQul.domain.entities.Datasource;
import com.regisx001.dQul.domain.enums.DatasourceStatus;

public interface DatasourceRepository extends JpaRepository<Datasource, UUID> {

    Optional<Datasource> findByName(String name);

    List<Datasource> findByStatus(DatasourceStatus status);

    List<Datasource> findByOwner(String owner);

    boolean existsByName(String name);
}
