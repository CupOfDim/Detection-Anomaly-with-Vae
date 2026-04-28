package com.natyrov.spring.detection_with_vae.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="analysis_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String taskName;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="dataset_id", nullable = false)
    private Dataset dataset;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String timeColumn;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String featureColumns;

    @Column(nullable = false)
    private Integer windowSize;

    @Column(nullable = false)
    private Integer stride;

    @Column(nullable = false)
    private Integer latentDim;

    @Column(nullable = false)
    private Integer epochs;

    @Column(nullable = false)
    private Integer batchSize;

    @Column(nullable = false)
    private Boolean autoThreshold;

    private Double thresholdValue;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String resultSummary;

    @Column(columnDefinition = "LONGTEXT")
    private String chartDataJson;

    @Column(nullable = false)
    private String modelType;

}
