package com.granttrack.progress.entity;

import com.granttrack.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * Read-only projection of the progress-module {@code progress_reports} table (authoritative copy
 * and all writes live in core-service). Finance reads the report linked to a milestone to know
 * whether the Compliance Officer has approved it, gating milestone verification / fund release.
 * {@code status} is mapped as a plain String so finance need not depend on the core status enum.
 */
@Entity
@Table(name = "progress_reports")
@Getter
@Setter
@NoArgsConstructor
@SQLRestriction("deleted = false")
public class ProgressReport extends BaseEntity {

    @Column(name = "award_id", nullable = false)
    private Long awardId;

    @Column(name = "milestone_id")
    private Long milestoneId;

    @Column(name = "period", length = 50)
    private String period;

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @Column(name = "status", nullable = false, length = 30)
    private String status;
}
