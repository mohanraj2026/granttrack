package com.granttrack.progress.repository;

import com.granttrack.progress.entity.Deliverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeliverableRepository extends JpaRepository<Deliverable, Long>, JpaSpecificationExecutor<Deliverable> {
}
