package com.granttrack.funding.service.impl;

import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.funding.dto.request.SponsorRequest;
import com.granttrack.funding.dto.response.SponsorResponse;
import com.granttrack.funding.entity.Sponsor;
import com.granttrack.funding.mapper.FundingMapper;
import com.granttrack.funding.repository.SponsorRepository;
import com.granttrack.funding.service.SponsorService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
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
public class SponsorServiceImpl implements SponsorService {

    private final SponsorRepository sponsorRepository;
    private final FundingMapper mapper;

    @Override
    @Transactional
    public SponsorResponse create(SponsorRequest request) {
        Sponsor sponsor = Sponsor.builder()
                .name(request.name())
                .type(request.type())
                .contactEmail(request.contactEmail())
                .phone(request.phone())
                .address(request.address())
                .website(request.website())
                .build();
        sponsor = sponsorRepository.save(sponsor);
        // Generate code from DB id
        sponsor.setSponsorCode("SP" + String.format("%06d", sponsor.getId()));
        return mapper.toResponse(sponsorRepository.save(sponsor));
    }

    @Override
    @Transactional
    public SponsorResponse update(Long id, SponsorRequest request) {
        Sponsor sponsor = find(id);
        sponsor.setName(request.name());
        sponsor.setType(request.type());
        sponsor.setContactEmail(request.contactEmail());
        sponsor.setPhone(request.phone());
        sponsor.setAddress(request.address());
        sponsor.setWebsite(request.website());
        return mapper.toResponse(sponsorRepository.save(sponsor));
    }

    @Override
    @Transactional(readOnly = true)
    public SponsorResponse getById(Long id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SponsorResponse> list(String q, Pageable pageable) {
        Specification<Sponsor> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(q)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return sponsorRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Sponsor sponsor = find(id);
        sponsor.setDeleted(true);
        sponsorRepository.save(sponsor);
    }

    private Sponsor find(Long id) {
        return sponsorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sponsor", id));
    }
}
