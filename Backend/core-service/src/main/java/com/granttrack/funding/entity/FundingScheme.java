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

/** A grant funding scheme owned by a {@link Sponsor}. */
@Entity
@Table(name = "funding_schemes", indexes = {
        @Index(name = "ix_funding_schemes_sponsor", columnList = "sponsor_id"),
        @Index(name = "ix_funding_schemes_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class FundingScheme extends BaseEntity {

    @Column(name = "scheme_code", unique = true, length = 20)
    private String schemeCode;

    @Column(name = "scheme_name", nullable = false, length = 200)
    private String schemeName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sponsor_id", nullable = false)
    private Sponsor sponsor;

    @Column(name = "research_area", length = 200)
    private String researchArea;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "max_award_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal maxAwardAmount;

    @Column(name = "min_award_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal minAwardAmount;

    @Column(name = "eligible_applicants", length = 500)
    private String eligibleApplicants;

    @Column(name = "funding_duration_months")
    private Integer fundingDurationMonths;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "document_path", length = 500)
    private String documentPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SchemeStatus status = SchemeStatus.ACTIVE;
}
