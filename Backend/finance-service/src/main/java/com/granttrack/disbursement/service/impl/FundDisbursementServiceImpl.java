package com.granttrack.disbursement.service.impl;

import com.granttrack.disbursement.dto.response.FundDisbursementResponse;
import com.granttrack.disbursement.entity.DisbursementStatus;
import com.granttrack.disbursement.entity.FundDisbursement;
import com.granttrack.disbursement.mapper.DisbursementMapper;
import com.granttrack.disbursement.repository.FundDisbursementRepository;
import com.granttrack.disbursement.service.FundDisbursementService;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.security.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FundDisbursementServiceImpl implements FundDisbursementService {

    private static final String[] STAFF_ROLES =
            {"ROLE_GRANT_ADMIN", "ROLE_ADMIN", "ROLE_FINANCE_OFFICER", "ROLE_COMPLIANCE_OFFICER"};

    private final FundDisbursementRepository disbursementRepository;
    private final DisbursementMapper mapper;
    private final com.granttrack.award.repository.GrantAwardRepository awardRepository;
    private final com.granttrack.application.repository.GrantApplicationRepository applicationRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<FundDisbursementResponse> search(Long awardId, Long milestoneId, String status, Pageable pageable) {
        // Staff see all fund releases; a researcher only those for their own applications' awards.
        boolean staff = SecurityUtils.hasAnyRole(STAFF_ROLES);
        List<Long> ownedAwardIds = staff ? null : ownedAwardIds();
        Specification<FundDisbursement> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!staff) {
                if (ownedAwardIds.isEmpty()) {
                    return cb.disjunction();
                }
                predicates.add(root.get("awardId").in(ownedAwardIds));
            }
            if (awardId != null) {
                predicates.add(cb.equal(root.get("awardId"), awardId));
            }
            if (milestoneId != null) {
                predicates.add(cb.equal(root.get("milestone").get("id"), milestoneId));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), parseStatus(status)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return disbursementRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    private DisbursementStatus parseStatus(String raw) {
        try {
            return DisbursementStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid disbursement status: " + raw);
        }
    }

    private List<Long> ownedAwardIds() {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(-1L);
        List<Long> appIds = applicationRepository.findIdsByPrincipalInvestigatorId(currentUserId);
        return appIds.isEmpty() ? appIds : awardRepository.findIdsByApplicationIdIn(appIds);
    }
}
