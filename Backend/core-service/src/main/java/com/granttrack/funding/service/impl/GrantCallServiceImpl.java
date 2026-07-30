package com.granttrack.funding.service.impl;

import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.funding.dto.request.GrantCallRequest;
import com.granttrack.funding.dto.response.GrantCallResponse;
import com.granttrack.funding.entity.CallStatus;
import com.granttrack.funding.entity.FundingScheme;
import com.granttrack.funding.entity.GrantCall;
import com.granttrack.funding.entity.ReviewMethod;
import com.granttrack.funding.entity.SchemeStatus;
import com.granttrack.funding.mapper.FundingMapper;
import com.granttrack.funding.repository.FundingSchemeRepository;
import com.granttrack.funding.repository.GrantCallRepository;
import com.granttrack.funding.service.GrantCallService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GrantCallServiceImpl implements GrantCallService {

    private final GrantCallRepository callRepository;
    private final FundingSchemeRepository schemeRepository;
    private final FundingMapper mapper;

    @Override
    @Transactional
    public GrantCallResponse create(GrantCallRequest request) {
        if (request.closeDate().isBefore(request.openDate())) {
            throw new BusinessException("closeDate cannot be before openDate");
        }
        FundingScheme scheme = schemeRepository.findById(request.schemeId())
                .orElseThrow(() -> new ResourceNotFoundException("FundingScheme", request.schemeId()));
        GrantCall call = GrantCall.builder()
                .scheme(scheme)
                .callTitle(request.callTitle())
                .openDate(request.openDate())
                .closeDate(request.closeDate())
                .expectedAwards(request.expectedAwards())
                .totalBudgetAllocated(request.totalBudgetAllocated())
                .reviewMethod(parseMethod(request.reviewMethod()))
                .status(CallStatus.UPCOMING)
                .build();
        return mapper.toResponse(callRepository.save(call));
    }

    @Override
    @Transactional
    public GrantCallResponse update(Long id, GrantCallRequest request) {
        GrantCall call = find(id);
        if (call.getStatus() == CallStatus.CLOSED || call.getStatus() == CallStatus.AWARDED) {
            throw new BusinessException("Cannot edit a call in status " + call.getStatus());
        }
        if (request.closeDate().isBefore(request.openDate())) {
            throw new BusinessException("closeDate cannot be before openDate");
        }
        call.setCallTitle(request.callTitle());
        call.setOpenDate(request.openDate());
        call.setCloseDate(request.closeDate());
        call.setExpectedAwards(request.expectedAwards());
        call.setTotalBudgetAllocated(request.totalBudgetAllocated());
        call.setReviewMethod(parseMethod(request.reviewMethod()));
        return mapper.toResponse(callRepository.save(call));
    }

    @Override
    @Transactional(readOnly = true)
    public GrantCallResponse getById(Long id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GrantCallResponse> search(String q, String status, Long schemeId, Pageable pageable) {
        Specification<GrantCall> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(q)) {
                predicates.add(cb.like(cb.lower(root.get("callTitle")), "%" + q.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), parseStatus(status)));
            }
            if (schemeId != null) {
                predicates.add(cb.equal(root.get("scheme").get("id"), schemeId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return callRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public GrantCallResponse open(Long id) {
        GrantCall call = find(id);
        if (call.getStatus() != CallStatus.UPCOMING) {
            throw new BusinessException("Only an UPCOMING call can be opened (current: " + call.getStatus() + ")");
        }
        // A call may only accept applications under an ACTIVE scheme and while it is not yet expired.
        if (call.getScheme().getStatus() != SchemeStatus.ACTIVE) {
            throw new BusinessException("Cannot open a call whose scheme is not ACTIVE (scheme is "
                    + call.getScheme().getStatus() + ")");
        }
        if (call.getCloseDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Cannot open a call whose close date has already passed");
        }
        call.setStatus(CallStatus.OPEN);
        return mapper.toResponse(callRepository.save(call));
    }

    @Override
    @Transactional
    public GrantCallResponse close(Long id) {
        GrantCall call = find(id);
        // Closing is only meaningful once a call has actually run (OPEN/UNDER_REVIEW/AWARDED).
        if (call.getStatus() != CallStatus.OPEN
                && call.getStatus() != CallStatus.UNDER_REVIEW
                && call.getStatus() != CallStatus.AWARDED) {
            throw new BusinessException("Cannot close a call in status " + call.getStatus());
        }
        call.setStatus(CallStatus.CLOSED);
        return mapper.toResponse(callRepository.save(call));
    }

    @Override
    @Transactional
    public GrantCallResponse terminate(Long id) {
        GrantCall call = find(id);
        // Termination is an abnormal early stop; a call that is already finished cannot be terminated.
        if (call.getStatus() == CallStatus.CLOSED
                || call.getStatus() == CallStatus.TERMINATED
                || call.getStatus() == CallStatus.AWARDED) {
            throw new BusinessException("Cannot terminate a call in status " + call.getStatus());
        }
        call.setStatus(CallStatus.TERMINATED);
        return mapper.toResponse(callRepository.save(call));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        GrantCall call = find(id);
        call.setDeleted(true);
        callRepository.save(call);
    }

    private GrantCall find(Long id) {
        return callRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GrantCall", id));
    }

    private ReviewMethod parseMethod(String raw) {
        try {
            return ReviewMethod.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid review method: " + raw);
        }
    }

    private CallStatus parseStatus(String raw) {
        try {
            return CallStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid call status: " + raw);
        }
    }
}
