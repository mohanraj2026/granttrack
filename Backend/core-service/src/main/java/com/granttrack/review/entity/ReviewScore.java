package com.granttrack.review.entity;

import com.granttrack.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/** A single reviewer's score for one {@link ReviewCriterion} under an assignment. */
@Entity
@Table(name = "review_scores",
        indexes = {
                @Index(name = "ix_scores_assignment", columnList = "assignment_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_score_assignment_criterion",
                        columnNames = {"assignment_id", "criterion"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class ReviewScore extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private ReviewerAssignment assignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterion", nullable = false, length = 30)
    private ReviewCriterion criterion;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "comments", length = 1000)
    private String comments;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_recommendation", length = 30)
    private OverallRecommendation overallRecommendation;

    @Column(name = "submitted_date")
    private Instant submittedDate;
}
