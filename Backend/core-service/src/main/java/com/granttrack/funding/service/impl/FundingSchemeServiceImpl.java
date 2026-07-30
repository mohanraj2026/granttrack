package com.granttrack.funding.service.impl;

import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.funding.dto.request.FundingSchemeRequest;
import com.granttrack.funding.dto.response.FundingSchemeResponse;
import com.granttrack.funding.entity.FundingScheme;
import com.granttrack.funding.entity.SchemeStatus;
import com.granttrack.funding.entity.Sponsor;
import com.granttrack.funding.mapper.FundingMapper;
import com.granttrack.funding.repository.FundingSchemeRepository;
import com.granttrack.funding.repository.SponsorRepository;
import com.granttrack.funding.service.FundingSchemeService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.granttrack.application.service.DocumentStorageService;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FundingSchemeServiceImpl implements FundingSchemeService {

    private final FundingSchemeRepository schemeRepository;
    private final SponsorRepository sponsorRepository;
    private final DocumentStorageService documentStorageService;
    private final FundingMapper mapper;

    @Override
    @Transactional
    public FundingSchemeResponse create(FundingSchemeRequest request) {
        validateAwardRange(request);
        Sponsor sponsor = sponsorRepository.findById(request.sponsorId())
                .orElseThrow(() -> new ResourceNotFoundException("Sponsor", request.sponsorId()));
        FundingScheme scheme = FundingScheme.builder()
                .schemeName(request.schemeName())
                .sponsor(sponsor)
                .researchArea(request.researchArea())
                .category(request.category())
                .maxAwardAmount(request.maxAwardAmount())
                .minAwardAmount(request.minAwardAmount())
                .eligibleApplicants(request.eligibleApplicants())
                .fundingDurationMonths(computeDuration(request))
                .fromDate(request.fromDate())
                .toDate(request.toDate())
                .description(request.description())
                .status(parseStatus(request.status(), SchemeStatus.ACTIVE))
                .build();
        scheme = schemeRepository.save(scheme);
        // Generate code from DB id
        scheme.setSchemeCode("SCH" + String.format("%05d", scheme.getId()));
        return mapper.toResponse(schemeRepository.save(scheme));
    }

    @Override
    @Transactional
    public FundingSchemeResponse update(Long id, FundingSchemeRequest request) {
        validateAwardRange(request);
        FundingScheme scheme = find(id);
        if (!scheme.getSponsor().getId().equals(request.sponsorId())) {
            Sponsor sponsor = sponsorRepository.findById(request.sponsorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sponsor", request.sponsorId()));
            scheme.setSponsor(sponsor);
        }
        scheme.setSchemeName(request.schemeName());
        scheme.setResearchArea(request.researchArea());
        scheme.setCategory(request.category());
        scheme.setMaxAwardAmount(request.maxAwardAmount());
        scheme.setMinAwardAmount(request.minAwardAmount());
        scheme.setEligibleApplicants(request.eligibleApplicants());
        scheme.setFundingDurationMonths(computeDuration(request));
        scheme.setFromDate(request.fromDate());
        scheme.setToDate(request.toDate());
        scheme.setDescription(request.description());
        if (StringUtils.hasText(request.status())) {
            scheme.setStatus(parseStatus(request.status(), scheme.getStatus()));
        }
        return mapper.toResponse(schemeRepository.save(scheme));
    }

    @Override
    @Transactional(readOnly = true)
    public FundingSchemeResponse getById(Long id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FundingSchemeResponse> search(String q, String status, Pageable pageable) {
        Specification<FundingScheme> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(q)) {
                String like = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("schemeName")), like),
                        cb.like(cb.lower(root.get("researchArea")), like)));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), parseStatus(status, null)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return schemeRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public FundingSchemeResponse changeStatus(Long id, String status) {
        FundingScheme scheme = find(id);
        scheme.setStatus(parseStatus(status, scheme.getStatus()));
        return mapper.toResponse(schemeRepository.save(scheme));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FundingScheme scheme = find(id);
        scheme.setDeleted(true);
        schemeRepository.save(scheme);
    }

    @Override
    @Transactional
    public FundingSchemeResponse uploadDocument(Long id, org.springframework.web.multipart.MultipartFile file) {
        FundingScheme scheme = find(id);
        String relativePath = documentStorageService.storeSchemeDocument(id, file);
        scheme.setDocumentPath(relativePath);
        return mapper.toResponse(schemeRepository.save(scheme));
    }

    @Override
    @Transactional(readOnly = true)
    public FundingSchemeService.SchemeDocument downloadDocument(Long id) {
        FundingScheme scheme = find(id);
        if (!org.springframework.util.StringUtils.hasText(scheme.getDocumentPath())) {
            throw new com.granttrack.common.exception.BusinessException("No document attached to this scheme");
        }
        org.springframework.core.io.Resource resource = documentStorageService.load(scheme.getDocumentPath());
        return new FundingSchemeService.SchemeDocument(resource, org.springframework.util.StringUtils.getFilename(scheme.getDocumentPath()));
    }

    private FundingScheme find(Long id) {
        return schemeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FundingScheme", id));
    }

    private void validateAwardRange(FundingSchemeRequest request) {
        if (request.minAwardAmount().compareTo(request.maxAwardAmount()) > 0) {
            throw new BusinessException("minAwardAmount cannot exceed maxAwardAmount");
        }
        if (request.fromDate() != null && request.toDate() != null
                && request.toDate().isBefore(request.fromDate())) {
            throw new BusinessException("toDate cannot be before fromDate");
        }
    }

    /** Auto-calculate months from fromDate/toDate, or fall back to explicit field. */
    private Integer computeDuration(FundingSchemeRequest request) {
        if (request.fromDate() != null && request.toDate() != null) {
            long months = ChronoUnit.MONTHS.between(request.fromDate(), request.toDate());
            return Math.max(1, (int) months);
        }
        return request.fundingDurationMonths();
    }

    private SchemeStatus parseStatus(String raw, SchemeStatus fallback) {
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        try {
            return SchemeStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid scheme status: " + raw);
        }
    }
}
