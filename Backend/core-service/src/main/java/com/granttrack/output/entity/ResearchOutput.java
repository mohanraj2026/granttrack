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

import java.time.LocalDate;

/** A research output (publication, dataset, software, etc.) produced under an award. */
@Entity
@Table(name = "research_outputs", indexes = {
        @Index(name = "ix_research_outputs_award", columnList = "award_id"),
        @Index(name = "ix_research_outputs_type", columnList = "type"),
        @Index(name = "ix_research_outputs_doi", columnList = "doi")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class ResearchOutput extends BaseEntity {

    @Column(name = "award_id", nullable = false)
    private Long awardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private OutputType type;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "authors", length = 1000)
    private String authors;

    @Column(name = "publication_venue", length = 250)
    private String publicationVenue;

    @Column(name = "doi", length = 100)
    private String doi;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "open_access_compliant", nullable = false)
    @Builder.Default
    private Boolean openAccessCompliant = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private OutputStatus status = OutputStatus.IN_PREPARATION;
}
