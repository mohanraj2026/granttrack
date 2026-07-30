package com.granttrack.disbursement.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A released fund payment against a {@link DisbursementMilestone}. */
@Entity
@Table(name = "fund_disbursements", indexes = {
        @Index(name = "ix_disbursements_milestone", columnList = "milestone_id"),
        @Index(name = "ix_disbursements_award", columnList = "award_id"),
        @Index(name = "ix_disbursements_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class FundDisbursement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "milestone_id", nullable = false)
    private DisbursementMilestone milestone;

    @Column(name = "award_id", nullable = false)
    private Long awardId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "disbursed_date")
    private LocalDate disbursedDate;

    @Column(name = "receiving_account_ref", length = 100)
    private String receivingAccountRef;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DisbursementStatus status = DisbursementStatus.PENDING;
}
