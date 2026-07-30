package com.granttrack.output.service.impl;

import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.award.entity.GrantAward;
import com.granttrack.award.repository.GrantAwardRepository;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.output.dto.request.ResearchOutputRequest;
import com.granttrack.output.dto.response.ResearchOutputResponse;
import com.granttrack.output.entity.OutputStatus;
import com.granttrack.output.entity.OutputType;
import com.granttrack.output.entity.ResearchOutput;
import com.granttrack.output.mapper.OutputMapper;
import com.granttrack.output.repository.ResearchOutputRepository;
import com.granttrack.output.service.ResearchOutputService;
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
public class ResearchOutputServiceImpl implements ResearchOutputService {

    private static final String[] STAFF_ROLES =
            {"ROLE_GRANT_ADMIN", "ROLE_ADMIN", "ROLE_FINANCE_OFFICER", "ROLE_COMPLIANCE_OFFICER"};

    private final ResearchOutputRepository outputRepository;
    private final OutputMapper mapper;
    private final GrantAwardRepository awardRepository;
    private final GrantApplicationRepository applicationRepository;

    @Override
    @Transactional
    public ResearchOutputResponse create(ResearchOutputRequest request) {
        // A research output may only be recorded by the award's principal investigator.
        // The award need only exist (any status) — publications legitimately appear after completion.
        assertOwningPrincipalInvestigator(awardOf(request.awardId()));
        ResearchOutput output = ResearchOutput.builder()
                .awardId(request.awardId())
                .type(parseType(request.type(), null))
                .title(request.title())
                .authors(request.authors())
                .publicationVenue(request.publicationVenue())
                .doi(request.doi())
                .publishedDate(request.publishedDate())
                .openAccessCompliant(request.openAccessCompliant() != null ? request.openAccessCompliant() : Boolean.FALSE)
                .status(parseStatus(request.status(), OutputStatus.IN_PREPARATION))
                .build();
        return mapper.toResponse(outputRepository.save(output));
    }

    @Override
    @Transactional
    public ResearchOutputResponse update(Long id, ResearchOutputRequest request) {
        ResearchOutput output = find(id);
        assertOwningPrincipalInvestigator(awardOf(output.getAwardId()));
        output.setAwardId(request.awardId());
        output.setType(parseType(request.type(), output.getType()));
        output.setTitle(request.title());
        output.setAuthors(request.authors());
        output.setPublicationVenue(request.publicationVenue());
        output.setDoi(request.doi());
        output.setPublishedDate(request.publishedDate());
        if (request.openAccessCompliant() != null) {
            output.setOpenAccessCompliant(request.openAccessCompliant());
        }
        if (StringUtils.hasText(request.status())) {
            output.setStatus(parseStatus(request.status(), output.getStatus()));
        }
        return mapper.toResponse(outputRepository.save(output));
    }

    @Override
    @Transactional(readOnly = true)
    public ResearchOutputResponse getById(Long id) {
        ResearchOutput output = find(id);
        assertCanReadAward(output.getAwardId());
        return mapper.toResponse(output);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResearchOutputResponse> search(Long awardId, String type, String status, String q, Pageable pageable) {
        boolean staff = SecurityUtils.hasAnyRole(STAFF_ROLES);
        List<Long> ownedAwardIds = staff ? null : ownedAwardIds();
        Specification<ResearchOutput> spec = (root, cq, cb) -> {
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
            if (StringUtils.hasText(type)) {
                predicates.add(cb.equal(root.get("type"), parseType(type, null)));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), parseStatus(status, null)));
            }
            if (StringUtils.hasText(q)) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + q.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return outputRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ResearchOutput output = find(id);
        assertCanModify(output.getAwardId());
        output.setDeleted(true);
        outputRepository.save(output);
    }

    private ResearchOutput find(Long id) {
        return outputRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchOutput", id));
    }

    private GrantAward awardOf(Long awardId) {
        return awardRepository.findById(awardId)
                .orElseThrow(() -> new ResourceNotFoundException("GrantAward", awardId));
    }

    private OutputType parseType(String raw, OutputType fallback) {
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        try {
            return OutputType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid output type: " + raw);
        }
    }

    private OutputStatus parseStatus(String raw, OutputStatus fallback) {
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        try {
            return OutputStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid output status: " + raw);
        }
    }

    /** Award ids belonging to the current researcher's applications (for read scoping). */
    private List<Long> ownedAwardIds() {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(-1L);
        List<Long> appIds = applicationRepository.findIdsByPrincipalInvestigatorId(currentUserId);
        return appIds.isEmpty() ? appIds : awardRepository.findIdsByApplicationIdIn(appIds);
    }

    /** Staff roles may read any award's outputs; a researcher only their own. */
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
            throw new AccessDeniedException("You do not have access to this award's research outputs");
        }
    }
}
