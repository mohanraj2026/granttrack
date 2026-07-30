package com.granttrack.award.repository;

import com.granttrack.award.entity.GrantAward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface GrantAwardRepository extends JpaRepository<GrantAward, Long>, JpaSpecificationExecutor<GrantAward> {

    boolean existsByApplicationId(Long applicationId);

    @Query("select w.id from GrantAward w where w.applicationId in :appIds")
    List<Long> findIdsByApplicationIdIn(@Param("appIds") Collection<Long> applicationIds);
}
