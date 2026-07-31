package com.regisx001.dQul.datasource.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.regisx001.dQul.dataset.domain.Dataset;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "datasources")
public class Datasource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DatasourceStatus status;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private LocalDateTime registrationDate;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @OneToMany(mappedBy = "datasource", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Dataset> datasets = new ArrayList<>();

    public void addDataset(Dataset dataset) {
        datasets.add(dataset);
        dataset.setDatasource(this);
    }

    public void removeDataset(Dataset dataset) {
        datasets.remove(dataset);
        dataset.setDatasource(null);
    }
}
