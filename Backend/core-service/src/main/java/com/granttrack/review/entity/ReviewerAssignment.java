package com.granttrack.review.entity;

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

import java.time.LocalDate;

/** Assignment of a single (blind) reviewer to a grant application. */
@Entity
@Table(name = "reviewer_assignments",
        indexes = {
                @Index(name = "ix_assignments_application", columnList = "application_id"),
                @Index(name = "ix_assignments_reviewer", columnList = "reviewer_id"),
                @Index(name = "ix_assignments_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_assignment_app_reviewer",
                        columnNames = {"application_id", "reviewer_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class ReviewerAssignment extends BaseEntity {

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    @Column(name = "review_deadline")
    private LocalDate reviewDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "conflict_screening_status", nullable = false, length = 20)
    @Builder.Default
    private ConflictScreeningStatus conflictScreeningStatus = ConflictScreeningStatus.CLEAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    /** Reviewer's reason when declining the assignment (surfaced to the Grant Admin). */
    @Column(name = "response_comment", length = 1000)
    private String responseComment;
}
