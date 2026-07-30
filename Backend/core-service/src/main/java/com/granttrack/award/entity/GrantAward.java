package com.granttrack.award.entity;

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

/** A grant award issued against a successful grant application. */
@Entity
@Table(name = "grant_awards", indexes = {
        @Index(name = "ix_awards_status", columnList = "status"),
        @Index(name = "ix_awards_finance_officer", columnList = "finance_officer_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_award_application", columnNames = "application_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class GrantAward extends BaseEntity {

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "awarded_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal awardedAmount;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "conditions_ref", length = 255)
    private String conditionsRef;

    @Column(name = "award_letter_date")
    private LocalDate awardLetterDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AwardStatus status = AwardStatus.ACTIVE;

    /** Finance Officer assigned (from the panel decision) to review this award for disbursement. */
    @Column(name = "finance_officer_id")
    private Long financeOfficerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "finance_review_status", nullable = false, length = 20)
    @Builder.Default
    private FinanceReviewStatus financeReviewStatus = FinanceReviewStatus.PENDING;

    @Column(name = "finance_review_comment", length = 1000)
    private String financeReviewComment;
}
