package com.granttrack.funding.service.impl;

import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.funding.dto.request.InstitutionRequest;
import com.granttrack.funding.dto.response.InstitutionResponse;
import com.granttrack.funding.entity.Institution;
import com.granttrack.funding.mapper.FundingMapper;
import com.granttrack.funding.repository.InstitutionRepository;
import com.granttrack.funding.service.InstitutionService;
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
public class InstitutionServiceImpl implements InstitutionService {

    private final InstitutionRepository institutionRepository;
    private final FundingMapper mapper;

    @Override
    @Transactional
    public InstitutionResponse create(InstitutionRequest request) {
        Institution institution = Institution.builder()
                .name(request.name())
                .type(request.type())
                .country(request.country())
                .universityName(request.universityName())
                .address(request.address())
                .city(request.city())
                .state(request.state())
                .pincode(request.pincode())
                .mobileNumber(request.mobileNumber())
                .email(request.email())
                .build();
        institution = institutionRepository.save(institution);
        institution.setInstitutionCode("INST" + String.format("%05d", institution.getId()));
        return mapper.toResponse(institutionRepository.save(institution));
    }

    @Override
    @Transactional
    public InstitutionResponse update(Long id, InstitutionRequest request) {
        Institution institution = find(id);
        institution.setName(request.name());
        institution.setType(request.type());
        institution.setCountry(request.country());
        institution.setUniversityName(request.universityName());
        institution.setAddress(request.address());
        institution.setCity(request.city());
        institution.setState(request.state());
        institution.setPincode(request.pincode());
        institution.setMobileNumber(request.mobileNumber());
        institution.setEmail(request.email());
        return mapper.toResponse(institutionRepository.save(institution));
    }

    @Override
    @Transactional(readOnly = true)
    public InstitutionResponse getById(Long id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstitutionResponse> list(String q, Pageable pageable) {
        Specification<Institution> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(q)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return institutionRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Institution institution = find(id);
        institution.setDeleted(true);
        institutionRepository.save(institution);
    }

    private Institution find(Long id) {
        return institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", id));
    }
}
