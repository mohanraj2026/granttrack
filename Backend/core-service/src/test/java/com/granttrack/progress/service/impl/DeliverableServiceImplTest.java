package com.granttrack.progress.service.impl;

import com.granttrack.application.entity.GrantApplication;
import com.granttrack.application.repository.GrantApplicationRepository;
import com.granttrack.award.entity.AwardStatus;
import com.granttrack.award.entity.GrantAward;
import com.granttrack.award.repository.GrantAwardRepository;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.notification.entity.NotificationCategory;
import com.granttrack.notification.service.NotificationService;
import com.granttrack.application.service.DocumentStorageService;
import com.granttrack.progress.dto.request.DeliverableRequest;
import com.granttrack.progress.dto.response.DeliverableResponse;
import com.granttrack.progress.entity.Deliverable;
import com.granttrack.progress.entity.DeliverableStatus;
import com.granttrack.progress.mapper.ProgressMapper;
import com.granttrack.progress.repository.DeliverableRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliverableServiceImplTest {

    private static final long PI = 100L;
    private static final long AWARD_ID = 1L;
    private static final long APP_ID = 1L;

    @Mock
    private DeliverableRepository deliverableRepository;
    @Mock
    private ProgressMapper mapper;
    @Mock
    private GrantAwardRepository awardRepository;
    @Mock
    private GrantApplicationRepository applicationRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private DocumentStorageService documentStorageService;

    @InjectMocks
    private DeliverableServiceImpl deliverableService;

    private final MultipartFile deliverableFile =
            new MockMultipartFile("file", "deliverable.pdf", "application/pdf", "data".getBytes());

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

    private GrantAward activeAward() {
        GrantAward award = GrantAward.builder().applicationId(APP_ID).status(AwardStatus.ACTIVE).build();
        award.setId(AWARD_ID);
        return award;
    }

    private GrantApplication piApp() {
        return GrantApplication.builder().principalInvestigatorId(PI).build();
    }

    private void wireOwnership() {
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(activeAward()));
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(piApp()));
    }

    private DeliverableRequest request() {
        return new DeliverableRequest(AWARD_ID, "Title", "REPORT", LocalDate.now());
    }

    @Test
    void create_Success() {
        loginAs(PI);
        wireOwnership();
        Deliverable deliverable = Deliverable.builder().build();
        DeliverableResponse response = DeliverableResponse.builder().id(1L).build();
        when(deliverableRepository.save(any(Deliverable.class))).thenReturn(deliverable);
        when(mapper.toResponse(deliverable)).thenReturn(response);

        assertNotNull(deliverableService.create(request()));
        verify(deliverableRepository, times(1)).save(any(Deliverable.class));
    }

    @Test
    void create_AwardNotActive_ThrowsException() {
        GrantAward award = GrantAward.builder().applicationId(APP_ID).status(AwardStatus.COMPLETED).build();
        award.setId(AWARD_ID);
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(award));

        assertThrows(BusinessException.class, () -> deliverableService.create(request()));
        verify(deliverableRepository, never()).save(any(Deliverable.class));
    }

    @Test
    void create_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        wireOwnership();

        assertThrows(AccessDeniedException.class, () -> deliverableService.create(request()));
        verify(deliverableRepository, never()).save(any(Deliverable.class));
    }

    @Test
    void upload_Pending_Success() {
        loginAs(PI);
        wireOwnership();
        Deliverable deliverable = Deliverable.builder().awardId(AWARD_ID).status(DeliverableStatus.PENDING).build();
        deliverable.setId(1L);
        DeliverableResponse response = DeliverableResponse.builder().id(1L).build();
        when(deliverableRepository.findById(1L)).thenReturn(Optional.of(deliverable));
        when(documentStorageService.storeDeliverable(eq(1L), any())).thenReturn("deliverables/1/x.pdf");
        when(deliverableRepository.save(any(Deliverable.class))).thenReturn(deliverable);
        when(mapper.toResponse(deliverable)).thenReturn(response);

        assertNotNull(deliverableService.upload(1L, deliverableFile));
        assertEquals(DeliverableStatus.SUBMITTED, deliverable.getStatus());
        assertEquals("deliverable.pdf", deliverable.getFileName());
    }

    @Test
    void upload_Rejected_ReuploadSuccess() {
        loginAs(PI);
        wireOwnership();
        Deliverable deliverable = Deliverable.builder().awardId(AWARD_ID).status(DeliverableStatus.REJECTED).build();
        deliverable.setId(1L);
        DeliverableResponse response = DeliverableResponse.builder().id(1L).build();
        when(deliverableRepository.findById(1L)).thenReturn(Optional.of(deliverable));
        when(documentStorageService.storeDeliverable(eq(1L), any())).thenReturn("deliverables/1/x2.pdf");
        when(deliverableRepository.save(any(Deliverable.class))).thenReturn(deliverable);
        when(mapper.toResponse(deliverable)).thenReturn(response);

        assertNotNull(deliverableService.upload(1L, deliverableFile));
        assertEquals(DeliverableStatus.SUBMITTED, deliverable.getStatus());
    }

    @Test
    void upload_Accepted_ThrowsException() {
        loginAs(PI);
        wireOwnership();
        Deliverable deliverable = Deliverable.builder().awardId(AWARD_ID).status(DeliverableStatus.ACCEPTED).build();
        deliverable.setId(1L);
        when(deliverableRepository.findById(1L)).thenReturn(Optional.of(deliverable));

        assertThrows(BusinessException.class,
                () -> deliverableService.upload(1L, deliverableFile));
        verify(deliverableRepository, never()).save(any(Deliverable.class));
    }

    @Test
    void upload_NotOwner_ThrowsAccessDenied() {
        loginAs(999L);
        wireOwnership();
        Deliverable deliverable = Deliverable.builder().awardId(AWARD_ID).status(DeliverableStatus.PENDING).build();
        deliverable.setId(1L);
        when(deliverableRepository.findById(1L)).thenReturn(Optional.of(deliverable));

        assertThrows(AccessDeniedException.class,
                () -> deliverableService.upload(1L, deliverableFile));
    }

    @Test
    void review_Accept_Success() {
        Deliverable deliverable = Deliverable.builder().awardId(AWARD_ID).title("Final Report").status(DeliverableStatus.SUBMITTED).build();
        deliverable.setId(1L);
        DeliverableResponse response = DeliverableResponse.builder().id(1L).build();
        when(deliverableRepository.findById(1L)).thenReturn(Optional.of(deliverable));
        when(deliverableRepository.save(any(Deliverable.class))).thenReturn(deliverable);
        when(mapper.toResponse(deliverable)).thenReturn(response);
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(activeAward()));
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(piApp()));

        assertNotNull(deliverableService.review(1L, "ACCEPT", "Looks good"));
        assertEquals(DeliverableStatus.ACCEPTED, deliverable.getStatus());
        assertEquals("Looks good", deliverable.getReviewComment());
        verify(notificationService, times(1)).notify(eq(PI), anyString(), eq(NotificationCategory.PROGRESS));
    }

    @Test
    void review_Reject_Success() {
        Deliverable deliverable = Deliverable.builder().awardId(AWARD_ID).title("Final Report").status(DeliverableStatus.SUBMITTED).build();
        deliverable.setId(1L);
        DeliverableResponse response = DeliverableResponse.builder().id(1L).build();
        when(deliverableRepository.findById(1L)).thenReturn(Optional.of(deliverable));
        when(deliverableRepository.save(any(Deliverable.class))).thenReturn(deliverable);
        when(mapper.toResponse(deliverable)).thenReturn(response);
        when(awardRepository.findById(AWARD_ID)).thenReturn(Optional.of(activeAward()));
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(piApp()));

        assertNotNull(deliverableService.review(1L, "REJECT", "Please revise section 3"));
        assertEquals(DeliverableStatus.REJECTED, deliverable.getStatus());
    }

    @Test
    void review_NotSubmitted_ThrowsException() {
        Deliverable deliverable = Deliverable.builder().status(DeliverableStatus.PENDING).build();
        deliverable.setId(1L);
        when(deliverableRepository.findById(1L)).thenReturn(Optional.of(deliverable));

        assertThrows(BusinessException.class, () -> deliverableService.review(1L, "ACCEPT", null));
    }

    @Test
    void getById_Owner_Success() {
        loginAs(PI);
        wireOwnership();
        Deliverable deliverable = Deliverable.builder().awardId(AWARD_ID).build();
        deliverable.setId(1L);
        DeliverableResponse response = DeliverableResponse.builder().id(1L).build();
        when(deliverableRepository.findById(1L)).thenReturn(Optional.of(deliverable));
        when(mapper.toResponse(deliverable)).thenReturn(response);

        assertNotNull(deliverableService.getById(1L));
    }

    @Test
    void getById_Staff_Success() {
        asStaff();
        Deliverable deliverable = Deliverable.builder().awardId(AWARD_ID).build();
        deliverable.setId(1L);
        DeliverableResponse response = DeliverableResponse.builder().id(1L).build();
        when(deliverableRepository.findById(1L)).thenReturn(Optional.of(deliverable));
        when(mapper.toResponse(deliverable)).thenReturn(response);

        assertNotNull(deliverableService.getById(1L));
        verify(awardRepository, never()).findById(anyLong());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(deliverableRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> deliverableService.getById(1L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_ResearcherScoped_Success() {
        loginAs(PI);
        Pageable pageable = PageRequest.of(0, 10);
        Deliverable deliverable = Deliverable.builder().build();
        Page<Deliverable> page = new PageImpl<>(List.of(deliverable));
        DeliverableResponse response = DeliverableResponse.builder().id(1L).build();
        when(applicationRepository.findIdsByPrincipalInvestigatorId(PI)).thenReturn(List.of(APP_ID));
        when(awardRepository.findIdsByApplicationIdIn(List.of(APP_ID))).thenReturn(List.of(AWARD_ID));
        when(deliverableRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(any(Deliverable.class))).thenReturn(response);

        Page<DeliverableResponse> result = deliverableService.search(AWARD_ID, "PENDING", pageable);

        assertEquals(1, result.getTotalElements());
    }
}
