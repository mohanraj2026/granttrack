package com.granttrack.output.repository;

import com.granttrack.output.entity.ResearchOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ResearchOutputRepository extends JpaRepository<ResearchOutput, Long>, JpaSpecificationExecutor<ResearchOutput> {
}
