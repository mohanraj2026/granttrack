package com.granttrack.application.entity;

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

/** A grant application submitted by a principal investigator against a grant call. */
@Entity
@Table(name = "grant_applications", indexes = {
        @Index(name = "ix_grant_applications_call", columnList = "call_id"),
        @Index(name = "ix_grant_applications_pi", columnList = "principal_investigator_id"),
        @Index(name = "ix_grant_applications_status", columnList = "status"),
        @Index(name = "ix_grant_applications_institution", columnList = "institution_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class GrantApplication extends BaseEntity {

    @Column(name = "call_id", nullable = false)
    private Long callId;

    @Column(name = "principal_investigator_id", nullable = false)
    private Long principalInvestigatorId;

    @Column(name = "project_title", nullable = false, length = 300)
    private String projectTitle;

    @Column(name = "research_abstract", columnDefinition = "TEXT")
    private String researchAbstract;

    @Column(name = "discipline", length = 150)
    private String discipline;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "project_duration_months")
    private Integer projectDurationMonths;

    @Column(name = "institution_id")
    private Long institutionId;

    @Column(name = "submission_date")
    private Instant submissionDate;

    /** Stored path/key of the uploaded abstract document (file stored on disk; path persisted). */
    @Column(name = "abstract_doc_path", length = 500)
    private String abstractDocPath;

    /** Original filename of the uploaded abstract document. */
    @Column(name = "abstract_doc_name", length = 255)
    private String abstractDocName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;
}
