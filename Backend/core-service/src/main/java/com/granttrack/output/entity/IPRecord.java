package com.granttrack.output.entity;

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
import java.time.LocalDate;

/** An intellectual-property record (patent, copyright, etc.) arising from an award. */
@Entity
@Table(name = "ip_records", indexes = {
        @Index(name = "ix_ip_records_award", columnList = "award_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class IPRecord extends BaseEntity {

    @Column(name = "award_id", nullable = false)
    private Long awardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ip_type", nullable = false, length = 20)
    private IpType ipType;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "inventors", length = 1000)
    private String inventors;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "grant_date")
    private LocalDate grantDate;

    @Column(name = "ownership_percent", precision = 5, scale = 2)
    private BigDecimal ownershipPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private IpStatus status = IpStatus.FILED;
}
