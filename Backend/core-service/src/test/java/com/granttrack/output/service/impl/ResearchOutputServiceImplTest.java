package com.granttrack.output.service.impl;

import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.award.entity.AwardStatus;
import com.granttrack.award.entity.GrantAward;
import com.granttrack.award.repository.GrantAwardRepository;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.output.dto.request.ResearchOutputRequest;
import com.granttrack.output.dto.response.ResearchOutputResponse;
import com.granttrack.output.entity.ResearchOutput;
import com.granttrack.output.mapper.OutputMapper;
import com.granttrack.output.repository.ResearchOutputRepository;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResearchOutputServiceImplTest {

    private static final long PI = 100L;
    private static final long AWARD_ID = 1L;
    private static final long APP_ID = 1L;

    @Mock
    private ResearchOutputRepository outputRepository;
    @Mock
    private OutputMapper mapper;
    @Mock
    private GrantAwardRepository awardRepository;
    @Mock
    private GrantApplicationRepository applicationRepository;

    @InjectMocks
    private ResearchOutputServiceImpl outputService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    private void loginAs(long id) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(id));
    }

    private void asStaff() {
        securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(any(String[].class))).thenReturn(true);
    }

    private void asAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("ROLE_ADMIN")).thenReturn(true);
    }

    private GrantAward award() {
        GrantAward award = GrantAward.builder().applicationId(APP_ID).status(AwardStatus.COMPLETED).build();
        award.setId(AWARD_ID);
        return award;
    }

    private GrantApplication piApp() {
        return GrantApplication.builder().principalInvestigatorId(PI).build();
    }

    private void wireOwnership() {
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(award()));
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(piApp()));
    }

    private ResearchOutputRequest request(String status) {
        return new ResearchOutputRequest(AWARD_ID, "JOURNAL_ARTICLE", "Title", "Authors", "Venue", "DOI", LocalDate.now(), true, status);
    }

    @Test
    void create_Success() {
        loginAs(PI);
        wireOwnership();
        ResearchOutput output = ResearchOutput.builder().build();
        ResearchOutputResponse response = ResearchOutputResponse.builder().id(1L).build();
        when(outputRepository.save(any(ResearchOutput.class))).thenReturn(output);
        when(mapper.toResponse(output)).thenReturn(response);

        assertNotNull(outputService.create(request("PUBLISHED")));
        verify(outputRepository, times(1)).save(any(ResearchOutput.class));
    }

    @Test
    void create_CompletedAward_Success() {
        // Award status COMPLETED is allowed — publications appear after the grant ends.
        loginAs(PI);
        wireOwnership();
        ResearchOutput output = ResearchOutput.builder().build();
        ResearchOutputResponse response = ResearchOutputResponse.builder().id(1L).build();
        when(outputRepository.save(any(ResearchOutput.class))).thenReturn(output);
        when(mapper.toResponse(output)).thenReturn(response);

        assertNotNull(outputService.create(request("PUBLISHED")));
    }

    @Test
    void create_AwardNotFound_ThrowsException() {
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> outputService.create(request("PUBLISHED")));
        verify(outputRepository, never()).save(any(ResearchOutput.class));
    }

    @Test
    void create_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        wireOwnership();

        assertThrows(AccessDeniedException.class, () -> outputService.create(request("PUBLISHED")));
        verify(outputRepository, never()).save(any(ResearchOutput.class));
    }

    @Test
    void create_InvalidType_ThrowsException() {
        loginAs(PI);
        wireOwnership();
        ResearchOutputRequest request = new ResearchOutputRequest(AWARD_ID, "INVALID", "Title", "Authors", "Venue", "DOI", LocalDate.now(), true, "PUBLISHED");

        assertThrows(BusinessException.class, () -> outputService.create(request));
    }

    @Test
    void update_Success() {
        loginAs(PI);
        wireOwnership();
        ResearchOutput output = ResearchOutput.builder().awardId(AWARD_ID).build();
        output.setId(1L);
        ResearchOutputResponse response = ResearchOutputResponse.builder().id(1L).build();
        when(outputRepository.findById(1L)).thenReturn(Optional.of(output));
        when(outputRepository.save(any(ResearchOutput.class))).thenReturn(output);
        when(mapper.toResponse(output)).thenReturn(response);

        assertNotNull(outputService.update(1L, request("PUBLISHED")));
        verify(outputRepository, times(1)).save(any(ResearchOutput.class));
    }

    @Test
    void update_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        wireOwnership();
        ResearchOutput output = ResearchOutput.builder().awardId(AWARD_ID).build();
        output.setId(1L);
        when(outputRepository.findById(1L)).thenReturn(Optional.of(output));

        assertThrows(AccessDeniedException.class, () -> outputService.update(1L, request("PUBLISHED")));
        verify(outputRepository, never()).save(any(ResearchOutput.class));
    }

    @Test
    void getById_Owner_Success() {
        loginAs(PI);
        wireOwnership();
        ResearchOutput output = ResearchOutput.builder().awardId(AWARD_ID).build();
        output.setId(1L);
        ResearchOutputResponse response = ResearchOutputResponse.builder().id(1L).build();
        when(outputRepository.findById(1L)).thenReturn(Optional.of(output));
        when(mapper.toResponse(output)).thenReturn(response);

        assertNotNull(outputService.getById(1L));
    }

    @Test
    void getById_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        wireOwnership();
        ResearchOutput output = ResearchOutput.builder().awardId(AWARD_ID).build();
        output.setId(1L);
        when(outputRepository.findById(1L)).thenReturn(Optional.of(output));

        assertThrows(AccessDeniedException.class, () -> outputService.getById(1L));
    }

    @Test
    void getById_Staff_Success() {
        asStaff();
        ResearchOutput output = ResearchOutput.builder().awardId(AWARD_ID).build();
        output.setId(1L);
        ResearchOutputResponse response = ResearchOutputResponse.builder().id(1L).build();
        when(outputRepository.findById(1L)).thenReturn(Optional.of(output));
        when(mapper.toResponse(output)).thenReturn(response);

        assertNotNull(outputService.getById(1L));
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(outputRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> outputService.getById(1L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_ResearcherScoped_Success() {
        loginAs(PI);
        Pageable pageable = PageRequest.of(0, 10);
        ResearchOutput output = ResearchOutput.builder().build();
        Page<ResearchOutput> page = new PageImpl<>(List.of(output));
        ResearchOutputResponse response = ResearchOutputResponse.builder().id(1L).build();
        when(applicationRepository.findIdsByPrincipalInvestigatorId(PI)).thenReturn(List.of(APP_ID));
        when(awardRepository.findIdsByApplicationIdIn(List.of(APP_ID))).thenReturn(List.of(AWARD_ID));
        when(outputRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(any(ResearchOutput.class))).thenReturn(response);

        Page<ResearchOutputResponse> result = outputService.search(AWARD_ID, "JOURNAL_ARTICLE", "PUBLISHED", "Title", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void delete_Owner_Success() {
        loginAs(PI);
        wireOwnership();
        ResearchOutput output = ResearchOutput.builder().awardId(AWARD_ID).build();
        output.setId(1L);
        when(outputRepository.findById(1L)).thenReturn(Optional.of(output));
        when(outputRepository.save(any(ResearchOutput.class))).thenReturn(output);

        outputService.delete(1L);

        assertEquals(true, output.isDeleted());
        verify(outputRepository, times(1)).save(any(ResearchOutput.class));
    }

    @Test
    void delete_Admin_Success() {
        asAdmin();
        ResearchOutput output = ResearchOutput.builder().awardId(AWARD_ID).build();
        output.setId(1L);
        when(outputRepository.findById(1L)).thenReturn(Optional.of(output));
        when(outputRepository.save(any(ResearchOutput.class))).thenReturn(output);

        outputService.delete(1L);

        verify(outputRepository, times(1)).save(any(ResearchOutput.class));
    }

    @Test
    void delete_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        wireOwnership();
        ResearchOutput output = ResearchOutput.builder().awardId(AWARD_ID).build();
        output.setId(1L);
        when(outputRepository.findById(1L)).thenReturn(Optional.of(output));

        assertThrows(AccessDeniedException.class, () -> outputService.delete(1L));
        verify(outputRepository, never()).save(any(ResearchOutput.class));
    }
}
