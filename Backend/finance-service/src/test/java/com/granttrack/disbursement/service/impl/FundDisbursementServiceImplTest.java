package com.granttrack.disbursement.service.impl;

import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.award.repository.GrantAwardRepository;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.disbursement.dto.response.FundDisbursementResponse;
import com.granttrack.disbursement.entity.FundDisbursement;
import com.granttrack.disbursement.mapper.DisbursementMapper;
import com.granttrack.disbursement.repository.FundDisbursementRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FundDisbursementServiceImplTest {

    private static final long PI = 2L;

    @Mock
    private FundDisbursementRepository disbursementRepository;
    @Mock
    private DisbursementMapper mapper;
    @Mock
    private GrantAwardRepository awardRepository;
    @Mock
    private GrantApplicationRepository applicationRepository;

    @InjectMocks
    private FundDisbursementServiceImpl fundDisbursementService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_ResearcherScoped_Success() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(PI));
        Pageable pageable = PageRequest.of(0, 10);
        FundDisbursement disbursement = FundDisbursement.builder().build();
        Page<FundDisbursement> page = new PageImpl<>(List.of(disbursement));
        FundDisbursementResponse response = FundDisbursementResponse.builder().id(1L).build();

        when(applicationRepository.findIdsByPrincipalInvestigatorId(PI)).thenReturn(List.of(5L));
        when(awardRepository.findIdsByApplicationIdIn(List.of(5L))).thenReturn(List.of(1L));
        when(disbursementRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(any(FundDisbursement.class))).thenReturn(response);

        Page<FundDisbursementResponse> result = fundDisbursementService.search(1L, 2L, "RELEASED", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
