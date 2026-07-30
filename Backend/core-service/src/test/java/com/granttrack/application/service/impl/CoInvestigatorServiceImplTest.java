package com.granttrack.application.service.impl;

import com.granttrack.application.dto.request.CoInvestigatorRequest;
import com.granttrack.application.dto.response.CoInvestigatorResponse;
import com.granttrack.application.entity.ApplicationStatus;
import com.granttrack.application.entity.CoInvestigator;
import com.granttrack.application.entity.CoInvestigatorRole;
import com.granttrack.application.entity.CoInvestigatorStatus;
import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.mapper.ApplicationMapper;
import com.granttrack.application.repository.CoInvestigatorRepository;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoInvestigatorServiceImplTest {

    private static final long OWNER = 2L;
    private static final long INVITEE = 5L;

    @Mock
    private CoInvestigatorRepository coInvestigatorRepository;

    @Mock
    private GrantApplicationRepository applicationRepository;

    @Mock
    private ApplicationMapper mapper;

    @InjectMocks
    private CoInvestigatorServiceImpl coInvestigatorService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(OWNER));
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    private GrantApplication draftApp(Long id) {
        GrantApplication app = GrantApplication.builder()
                .principalInvestigatorId(OWNER).status(ApplicationStatus.DRAFT).build();
        app.setId(id);
        return app;
    }

    @Test
    void add_SetsInvitedStatus_Success() {
        Long appId = 1L;
        CoInvestigatorRequest request = new CoInvestigatorRequest(INVITEE, 1L, "CO_INVESTIGATOR", "Contribution");
        CoInvestigator coInvestigator = CoInvestigator.builder()
                .role(CoInvestigatorRole.CO_INVESTIGATOR).status(CoInvestigatorStatus.INVITED).build();
        coInvestigator.setId(1L);
        CoInvestigatorResponse response = CoInvestigatorResponse.builder().id(1L).role("CO_INVESTIGATOR").build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(draftApp(appId)));
        when(coInvestigatorRepository.save(any(CoInvestigator.class))).thenReturn(coInvestigator);
        when(mapper.toResponse(coInvestigator)).thenReturn(response);

        CoInvestigatorResponse result = coInvestigatorService.add(appId, request);

        assertNotNull(result);
        // Newly added members must be persisted as INVITED, not auto-confirmed.
        verify(coInvestigatorRepository).save(argThat(c -> c.getStatus() == CoInvestigatorStatus.INVITED));
    }

    @Test
    void add_NoIdentifier_ThrowsException() {
        Long appId = 1L;
        CoInvestigatorRequest request = new CoInvestigatorRequest(null, null, "CO_INVESTIGATOR", "c");

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(draftApp(appId)));

        assertThrows(BusinessException.class, () -> coInvestigatorService.add(appId, request));
        verify(coInvestigatorRepository, never()).save(any(CoInvestigator.class));
    }

    @Test
    void add_InvalidRole_ThrowsException() {
        Long appId = 1L;
        CoInvestigatorRequest request = new CoInvestigatorRequest(INVITEE, 1L, "INVALID", "Contribution");

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(draftApp(appId)));

        assertThrows(BusinessException.class, () -> coInvestigatorService.add(appId, request));
    }

    @Test
    void respond_Accept_Success() {
        Long appId = 1L;
        Long coiId = 9L;
        // The invited user is acting.
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(INVITEE));
        CoInvestigator coi = CoInvestigator.builder()
                .application(draftApp(appId)).userId(INVITEE).status(CoInvestigatorStatus.INVITED).build();
        coi.setId(coiId);
        CoInvestigatorResponse response = CoInvestigatorResponse.builder().id(coiId).status("CONFIRMED").build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(draftApp(appId)));
        when(coInvestigatorRepository.findById(coiId)).thenReturn(Optional.of(coi));
        when(coInvestigatorRepository.save(any(CoInvestigator.class))).thenReturn(coi);
        when(mapper.toResponse(coi)).thenReturn(response);

        CoInvestigatorResponse result = coInvestigatorService.respond(appId, coiId, "ACCEPT");

        assertNotNull(result);
        assertEquals(CoInvestigatorStatus.CONFIRMED, coi.getStatus());
    }

    @Test
    void respond_NotInvitedUser_ThrowsAccessDenied() {
        Long appId = 1L;
        Long coiId = 9L;
        // Current user (OWNER) is not the invited user (INVITEE).
        CoInvestigator coi = CoInvestigator.builder()
                .application(draftApp(appId)).userId(INVITEE).status(CoInvestigatorStatus.INVITED).build();
        coi.setId(coiId);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(draftApp(appId)));
        when(coInvestigatorRepository.findById(coiId)).thenReturn(Optional.of(coi));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> coInvestigatorService.respond(appId, coiId, "ACCEPT"));
    }

    @Test
    void listByApplication_Success() {
        Long appId = 1L;
        CoInvestigator coInvestigator = CoInvestigator.builder().build();
        CoInvestigatorResponse response = CoInvestigatorResponse.builder().id(1L).build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(draftApp(appId)));
        when(coInvestigatorRepository.findByApplicationId(appId)).thenReturn(List.of(coInvestigator));
        when(mapper.toResponse(coInvestigator)).thenReturn(response);

        List<CoInvestigatorResponse> result = coInvestigatorService.listByApplication(appId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
