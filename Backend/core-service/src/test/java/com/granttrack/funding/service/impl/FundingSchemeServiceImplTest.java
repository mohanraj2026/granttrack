package com.granttrack.funding.service.impl;

import com.granttrack.application.service.DocumentStorageService;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.funding.dto.request.FundingSchemeRequest;
import com.granttrack.funding.dto.response.FundingSchemeResponse;
import com.granttrack.funding.entity.FundingScheme;
import com.granttrack.funding.entity.SchemeStatus;
import com.granttrack.funding.entity.Sponsor;
import com.granttrack.funding.mapper.FundingMapper;
import com.granttrack.funding.repository.FundingSchemeRepository;
import com.granttrack.funding.repository.SponsorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingSchemeServiceImplTest {

    @Mock
    private FundingSchemeRepository schemeRepository;

    @Mock
    private SponsorRepository sponsorRepository;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private FundingMapper mapper;

    @InjectMocks
    private FundingSchemeServiceImpl schemeService;

    @Test
    void create_Success() {
        FundingSchemeRequest request = new FundingSchemeRequest(
                "Scheme", 1L, "Area", "Cat",
                new BigDecimal("100000"), new BigDecimal("10000"),
                "Eligible", 12, LocalDate.now(), LocalDate.now().plusMonths(12),
                "Desc", "ACTIVE"
        );
        Sponsor sponsor = Sponsor.builder().name("Sponsor").build();
        sponsor.setId(1L);
        FundingScheme savedScheme = FundingScheme.builder().schemeName("Scheme").sponsor(sponsor).build();
        savedScheme.setId(1L);
        FundingSchemeResponse response = FundingSchemeResponse.builder().id(1L).schemeName("Scheme").build();

        when(sponsorRepository.findById(1L)).thenReturn(Optional.of(sponsor));
        when(schemeRepository.save(any(FundingScheme.class))).thenReturn(savedScheme);
        when(mapper.toResponse(savedScheme)).thenReturn(response);

        FundingSchemeResponse result = schemeService.create(request);

        assertNotNull(result);
        assertEquals("Scheme", result.schemeName());
        verify(schemeRepository, times(2)).save(any(FundingScheme.class));
    }

    @Test
    void create_InvalidAwardRange_ThrowsException() {
        FundingSchemeRequest request = new FundingSchemeRequest(
                "Scheme", 1L, "Area", "Cat",
                new BigDecimal("10000"), new BigDecimal("100000"), // min > max
                "Eligible", 12, null, null,
                "Desc", "ACTIVE"
        );

        assertThrows(BusinessException.class, () -> schemeService.create(request));
    }

    @Test
    void create_InvalidDateRange_ThrowsException() {
        FundingSchemeRequest request = new FundingSchemeRequest(
                "Scheme", 1L, "Area", "Cat",
                new BigDecimal("10000"), new BigDecimal("100000"),
                "Eligible", 12, LocalDate.now(), LocalDate.now().minusDays(1), // toDate before fromDate
                "Desc", "ACTIVE"
        );

        assertThrows(BusinessException.class, () -> schemeService.create(request));
        verify(schemeRepository, never()).save(any(FundingScheme.class));
    }

    @Test
    void update_Success() {
        Long schemeId = 1L;
        FundingSchemeRequest request = new FundingSchemeRequest(
                "Upd Scheme", 1L, "Area", "Cat",
                new BigDecimal("100000"), new BigDecimal("10000"),
                "Eligible", 12, null, null,
                "Desc", "ACTIVE"
        );
        Sponsor sponsor = Sponsor.builder().name("Sponsor").build();
        sponsor.setId(1L);
        FundingScheme existingScheme = FundingScheme.builder().schemeName("Old").sponsor(sponsor).build();
        existingScheme.setId(schemeId);
        FundingSchemeResponse response = FundingSchemeResponse.builder().id(schemeId).schemeName("Upd Scheme").build();

        when(schemeRepository.findById(schemeId)).thenReturn(Optional.of(existingScheme));
        when(schemeRepository.save(any(FundingScheme.class))).thenReturn(existingScheme);
        when(mapper.toResponse(existingScheme)).thenReturn(response);

        FundingSchemeResponse result = schemeService.update(schemeId, request);

        assertNotNull(result);
        assertEquals("Upd Scheme", result.schemeName());
    }

    @Test
    void getById_Success() {
        Long schemeId = 1L;
        FundingScheme scheme = FundingScheme.builder().schemeName("Scheme").build();
        scheme.setId(schemeId);
        FundingSchemeResponse response = FundingSchemeResponse.builder().id(schemeId).schemeName("Scheme").build();

        when(schemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));
        when(mapper.toResponse(scheme)).thenReturn(response);

        FundingSchemeResponse result = schemeService.getById(schemeId);

        assertNotNull(result);
        assertEquals(schemeId, result.id());
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        FundingScheme scheme = FundingScheme.builder().schemeName("Scheme").build();
        scheme.setId(1L);
        FundingSchemeResponse response = FundingSchemeResponse.builder().id(1L).schemeName("Scheme").build();
        Page<FundingScheme> page = new PageImpl<>(List.of(scheme));

        when(schemeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(scheme)).thenReturn(response);

        Page<FundingSchemeResponse> result = schemeService.search("Scheme", "ACTIVE", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void changeStatus_Success() {
        Long schemeId = 1L;
        FundingScheme scheme = FundingScheme.builder().status(SchemeStatus.ACTIVE).build();
        scheme.setId(schemeId);
        FundingSchemeResponse response = FundingSchemeResponse.builder().id(schemeId).status("CLOSED").build();

        when(schemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));
        when(schemeRepository.save(any(FundingScheme.class))).thenReturn(scheme);
        when(mapper.toResponse(scheme)).thenReturn(response);

        FundingSchemeResponse result = schemeService.changeStatus(schemeId, "CLOSED");

        assertNotNull(result);
        assertEquals("CLOSED", result.status());
    }

    @Test
    void delete_Success() {
        Long schemeId = 1L;
        FundingScheme scheme = FundingScheme.builder().build();
        scheme.setId(schemeId);
        scheme.setDeleted(false);

        when(schemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));

        schemeService.delete(schemeId);

        assertTrue(scheme.isDeleted());
        verify(schemeRepository, times(1)).save(scheme);
    }

    @Test
    void uploadDocument_Success() {
        Long schemeId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        FundingScheme scheme = FundingScheme.builder().build();
        scheme.setId(schemeId);
        FundingSchemeResponse response = FundingSchemeResponse.builder().id(schemeId).build();

        when(schemeRepository.findById(schemeId)).thenReturn(Optional.of(scheme));
        when(documentStorageService.storeSchemeDocument(schemeId, file)).thenReturn("path");
        when(schemeRepository.save(any(FundingScheme.class))).thenReturn(scheme);
        when(mapper.toResponse(scheme)).thenReturn(response);

        FundingSchemeResponse result = schemeService.uploadDocument(schemeId, file);

        assertNotNull(result);
        assertEquals("path", scheme.getDocumentPath());
    }
}
