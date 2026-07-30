package com.granttrack.disbursement.entity;

import com.granttrack.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A payment milestone scheduled against a grant award. */
@Entity
@Table(name = "disbursement_milestones", indexes = {
        @Index(name = "ix_milestones_award", columnList = "award_id"),
        @Index(name = "ix_milestones_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_milestone_award_number", columnNames = {"award_id", "milestone_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class DisbursementMilestone extends BaseEntity {

    @Column(name = "award_id", nullable = false)
    private Long awardId;

    @Column(name = "milestone_number", nullable = false)
    private Integer milestoneNumber;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "evidence_required", nullable = false)
    @Builder.Default
    private Boolean evidenceRequired = true;

    @Column(name = "evidence_note", length = 1000)
    private String evidenceNote;

    @Column(name = "evidence_doc_path", length = 500)
    private String evidenceDocPath;

    @Column(name = "evidence_doc_name", length = 255)
    private String evidenceDocName;

    @Column(name = "evidence_submitted_date")
    private LocalDate evidenceSubmittedDate;

    /** Finance officer's reason when returning evidence for resubmission. */
    @Column(name = "evidence_review_comment", length = 1000)
    private String evidenceReviewComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MilestoneStatus status = MilestoneStatus.UPCOMING;
}
