package com.granttrack.funding.entity;

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

/** A submission window opened under a {@link FundingScheme}. */
@Entity
@Table(name = "grant_calls", indexes = {
        @Index(name = "ix_grant_calls_scheme", columnList = "scheme_id"),
        @Index(name = "ix_grant_calls_status", columnList = "status"),
        @Index(name = "ix_grant_calls_window", columnList = "open_date,close_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class GrantCall extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    private FundingScheme scheme;

    @Column(name = "call_title", nullable = false, length = 250)
    private String callTitle;

    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    @Column(name = "close_date", nullable = false)
    private LocalDate closeDate;

    @Column(name = "expected_awards")
    private Integer expectedAwards;

    @Column(name = "total_budget_allocated", precision = 15, scale = 2)
    private BigDecimal totalBudgetAllocated;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_method", nullable = false, length = 20)
    private ReviewMethod reviewMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CallStatus status = CallStatus.UPCOMING;
}
