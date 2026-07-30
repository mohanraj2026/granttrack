package com.granttrack.application.entity;

import com.granttrack.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * Read-only projection of the application-owned {@code grant_applications} table. Finance only
 * needs the principal-investigator id (to resolve who a milestone's notifications go to and to
 * read-scope a researcher's own disbursements), so only that column is mapped.
 */
@Entity
@Table(name = "grant_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class GrantApplication extends BaseEntity {

    @Column(name = "principal_investigator_id", nullable = false)
    private Long principalInvestigatorId;
}
