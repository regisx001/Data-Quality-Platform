package com.regisx001.dQul.dataset.domain;

import java.time.LocalDateTime;
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
@Table(name = "column_profiles")
public class ColumnProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id", nullable = false)
    @JsonIgnoreProperties({"dataset"})
    private DatasetColumn column;

    @Column(name = "null_count", nullable = false)
    @Builder.Default
    private Long nullCount = 0L;

    @Column(name = "null_percentage", nullable = false)
    @Builder.Default
    private Double nullPercentage = 0.0;

    @Column(name = "distinct_count", nullable = false)
    @Builder.Default
    private Long distinctCount = 0L;

    @Column(name = "min_value")
    private String minValue;

    @Column(name = "max_value")
    private String maxValue;

    @Column(name = "avg_value")
    private Double avgValue;

    @Column(name = "profiled_at", nullable = false)
    @Builder.Default
    private LocalDateTime profiledAt = LocalDateTime.now();
}
