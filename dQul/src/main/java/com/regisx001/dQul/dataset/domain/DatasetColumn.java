package com.regisx001.dQul.dataset.domain;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "dataset_columns")
public class DatasetColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @Column(name = "is_nullable", nullable = false)
    @Builder.Default
    private boolean isNullable = true;

    @Column(name = "is_primary_key", nullable = false)
    @Builder.Default
    private boolean isPrimaryKey = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    @JsonIgnoreProperties({"columns", "qualityRules", "validations", "datasource"})
    private Dataset dataset;

    @jakarta.persistence.OneToMany(mappedBy = "column", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties({"column"})
    private java.util.List<ColumnProfile> profiles = new java.util.ArrayList<>();
}
