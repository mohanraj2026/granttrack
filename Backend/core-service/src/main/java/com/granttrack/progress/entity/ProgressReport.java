package com.granttrack.progress.entity;

import com.granttrack.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;

/** A periodic progress report submitted against a grant award. */
@Entity
@Table(name = "progress_reports", indexes = {
        @Index(name = "ix_progress_award", columnList = "award_id"),
        @Index(name = "ix_progress_status", columnList = "status"),
        @Index(name = "ix_progress_submitted_by", columnList = "submitted_by_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class ProgressReport extends BaseEntity {

    @Column(name = "award_id", nullable = false)
    private Long awardId;

    /**
     * The disbursement milestone this report is the proof for (nullable — a periodic report
     * may be filed without being tied to a specific milestone). Owned/read by finance-service.
     */
    @Column(name = "milestone_id")
    private Long milestoneId;

    @Column(name = "period", length = 50)
    private String period;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "key_achievements", columnDefinition = "TEXT")
    private String keyAchievements;

    @Column(name = "challenges", columnDefinition = "TEXT")
    private String challenges;

    @Column(name = "budget_utilisation_percent", precision = 5, scale = 2)
    private BigDecimal budgetUtilisationPercent;

    @Column(name = "submitted_by_id")
    private Long submittedById;

    @Column(name = "submitted_date")
    private Instant submittedDate;

    @Column(name = "report_doc_path", length = 500)
    private String reportDocPath;

    @Column(name = "report_doc_name", length = 255)
    private String reportDocName;

    /** Compliance officer's comment on approve / request-revision. */
    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ProgressStatus status = ProgressStatus.DRAFT;
}
