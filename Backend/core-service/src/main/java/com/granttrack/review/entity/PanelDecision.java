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

import java.math.BigDecimal;
import java.time.LocalDate;

/** Final panel award decision for an application (one per application). */
@Entity
@Table(name = "panel_decisions",
        indexes = {
                @Index(name = "ix_panel_decided_by", columnList = "decided_by_id"),
                @Index(name = "ix_panel_finance_officer", columnList = "finance_officer_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_panel_application", columnNames = {"application_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class PanelDecision extends BaseEntity {

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "panel_date")
    private LocalDate panelDate;

    @Column(name = "consensus_score", precision = 5, scale = 2)
    private BigDecimal consensusScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "award_decision", nullable = false, length = 20)
    private AwardDecision awardDecision;

    @Column(name = "awarded_amount", precision = 15, scale = 2)
    private BigDecimal awardedAmount;

    @Column(name = "conditions_attached", length = 1000)
    private String conditionsAttached;

    @Column(name = "decided_by_id")
    private Long decidedById;

    /** Finance Officer assigned to handle disbursement for the awarded application. */
    @Column(name = "finance_officer_id")
    private Long financeOfficerId;
}
