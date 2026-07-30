package com.granttrack.output.service.impl;

import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.award.entity.GrantAward;
import com.granttrack.award.repository.GrantAwardRepository;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.output.dto.request.IPRecordRequest;
import com.granttrack.output.dto.response.IPRecordResponse;
import com.granttrack.output.entity.IPRecord;
import com.granttrack.output.entity.IpStatus;
import com.granttrack.output.entity.IpType;
import com.granttrack.output.mapper.OutputMapper;
import com.granttrack.output.repository.IPRecordRepository;
import com.granttrack.output.service.IPRecordService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IPRecordServiceImpl implements IPRecordService {

    private static final String[] STAFF_ROLES =
            {"ROLE_GRANT_ADMIN", "ROLE_ADMIN", "ROLE_FINANCE_OFFICER", "ROLE_COMPLIANCE_OFFICER"};

    private final IPRecordRepository ipRecordRepository;
    private final OutputMapper mapper;
    private final GrantAwardRepository awardRepository;
    private final GrantApplicationRepository applicationRepository;

    @Override
    @Transactional
    public IPRecordResponse create(IPRecordRequest request) {
        validateDates(request);
        // An IP record may only be recorded by the award's principal investigator.
        // The award need only exist (any status) — granted patents legitimately appear after completion.
        assertOwningPrincipalInvestigator(awardOf(request.awardId()));
        IPRecord record = IPRecord.builder()
                .awardId(request.awardId())
                .ipType(parseType(request.ipType(), null))
                .title(request.title())
                .inventors(request.inventors())
                .filingDate(request.filingDate())
                .grantDate(request.grantDate())
                .ownershipPercent(request.ownershipPercent())
                .status(parseStatus(request.status(), IpStatus.FILED))
                .build();
        return mapper.toResponse(ipRecordRepository.save(record));
    }

    @Override
    @Transactional
    public IPRecordResponse update(Long id, IPRecordRequest request) {
        validateDates(request);
        IPRecord record = find(id);
        assertOwningPrincipalInvestigator(awardOf(record.getAwardId()));
        record.setAwardId(request.awardId());
        record.setIpType(parseType(request.ipType(), record.getIpType()));
        record.setTitle(request.title());
        record.setInventors(request.inventors());
        record.setFilingDate(request.filingDate());
        record.setGrantDate(request.grantDate());
        record.setOwnershipPercent(request.ownershipPercent());
        if (StringUtils.hasText(request.status())) {
            record.setStatus(parseStatus(request.status(), record.getStatus()));
        }
        return mapper.toResponse(ipRecordRepository.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public IPRecordResponse getById(Long id) {
        IPRecord record = find(id);
        assertCanReadAward(record.getAwardId());
        return mapper.toResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IPRecordResponse> list(Long awardId, String status, Pageable pageable) {
        boolean staff = SecurityUtils.hasAnyRole(STAFF_ROLES);
        List<Long> ownedAwardIds = staff ? null : ownedAwardIds();
        Specification<IPRecord> spec = (root, cq, cb) -> {
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
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), parseStatus(status, null)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return ipRecordRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        IPRecord record = find(id);
        assertCanModify(record.getAwardId());
        record.setDeleted(true);
        ipRecordRepository.save(record);
    }

    private IPRecord find(Long id) {
        return ipRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IPRecord", id));
    }

    private GrantAward awardOf(Long awardId) {
        return awardRepository.findById(awardId)
                .orElseThrow(() -> new ResourceNotFoundException("GrantAward", awardId));
    }

    private void validateDates(IPRecordRequest request) {
        if (request.filingDate() != null && request.grantDate() != null
                && request.grantDate().isBefore(request.filingDate())) {
            throw new BusinessException("grantDate cannot be before filingDate");
        }
    }

    private IpType parseType(String raw, IpType fallback) {
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        try {
            return IpType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid IP type: " + raw);
        }
    }

    private IpStatus parseStatus(String raw, IpStatus fallback) {
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        try {
            return IpStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid IP status: " + raw);
        }
    }

    /** Award ids belonging to the current researcher's applications (for read scoping). */
    private List<Long> ownedAwardIds() {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(-1L);
        List<Long> appIds = applicationRepository.findIdsByPrincipalInvestigatorId(currentUserId);
        return appIds.isEmpty() ? appIds : awardRepository.findIdsByApplicationIdIn(appIds);
    }

    /** Staff roles may read any award's IP records; a researcher only their own. */
    private void assertCanReadAward(Long awardId) {
        if (SecurityUtils.hasAnyRole(STAFF_ROLES)) {
            return;
        }
        assertOwningPrincipalInvestigator(awardOf(awardId));
    }

    /** Deletion is permitted for a platform ADMIN, or the owning principal investigator. */
    private void assertCanModify(Long awardId) {
        if (SecurityUtils.hasAnyRole("ROLE_ADMIN")) {
            return;
        }
        assertOwningPrincipalInvestigator(awardOf(awardId));
    }

    /** The current caller must be the principal investigator of the award's application. */
    private void assertOwningPrincipalInvestigator(GrantAward award) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        Long piId = applicationRepository.findById(award.getApplicationId())
                .map(GrantApplication::getPrincipalInvestigatorId)
                .orElse(null);
        if (currentUserId == null || !currentUserId.equals(piId)) {
            throw new AccessDeniedException("You do not have access to this award's IP records");
        }
    }
}
