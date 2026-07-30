package com.granttrack.progress.entity;

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

/** A contractual deliverable due under a grant award. */
@Entity
@Table(name = "deliverables", indexes = {
        @Index(name = "ix_deliverables_award", columnList = "award_id"),
        @Index(name = "ix_deliverables_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class Deliverable extends BaseEntity {

    @Column(name = "award_id", nullable = false)
    private Long awardId;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private DeliverableType type;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "submitted_date")
    private LocalDate submittedDate;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_name", length = 255)
    private String fileName;

    /** Compliance officer's comment on accept / reject. */
    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DeliverableStatus status = DeliverableStatus.PENDING;
}
