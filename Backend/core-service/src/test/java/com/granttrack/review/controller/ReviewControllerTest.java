package com.granttrack.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.granttrack.review.dto.request.PanelDecisionRequest;
import com.granttrack.review.dto.request.ReviewScoreRequest;
import com.granttrack.review.dto.request.ReviewerAssignmentRequest;
import com.granttrack.review.dto.response.PanelDecisionResponse;
import com.granttrack.review.dto.response.ReviewScoreResponse;
import com.granttrack.review.dto.response.ReviewerAssignmentResponse;
import com.granttrack.review.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean(name = "auditorAware")
    private org.springframework.data.domain.AuditorAware<Long> auditorAware;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.granttrack.common.security.JwtTokenValidator jwtTokenValidator;


    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password", List.of())
        );
    }

    @Test
    void assign_Success() throws Exception {
        ReviewerAssignmentRequest request = new ReviewerAssignmentRequest(1L, 2L, LocalDate.now());
        ReviewerAssignmentResponse response = ReviewerAssignmentResponse.builder().id(1L).status("ASSIGNED").build();

        when(reviewService.assignReviewer(any(ReviewerAssignmentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/reviews/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));

        verify(reviewService, times(1)).assignReviewer(any(ReviewerAssignmentRequest.class));
    }

    @Test
    void conflictCheck_Success() throws Exception {
        ReviewerAssignmentResponse response = ReviewerAssignmentResponse.builder().id(1L).conflictScreeningStatus("CLEAR").build();

        when(reviewService.recordConflictCheck(1L, "CLEAR")).thenReturn(response);

        mockMvc.perform(post("/api/v1/reviews/assignments/1/conflict-check")
                        .param("status", "CLEAR")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.conflictScreeningStatus").value("CLEAR"));

        verify(reviewService, times(1)).recordConflictCheck(1L, "CLEAR");
    }

    @Test
    void respond_Success() throws Exception {
        ReviewerAssignmentResponse response = ReviewerAssignmentResponse.builder().id(1L).status("ACCEPTED").build();

        when(reviewService.respond(1L, "ACCEPT", null)).thenReturn(response);

        mockMvc.perform(post("/api/v1/reviews/assignments/1/respond")
                        .param("decision", "ACCEPT")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        verify(reviewService, times(1)).respond(1L, "ACCEPT", null);
    }

    @Test
    void submitScore_Success() throws Exception {
        ReviewScoreRequest request = new ReviewScoreRequest("SCIENTIFIC_MERIT", 9, "Good", "RECOMMENDED");
        ReviewScoreResponse response = ReviewScoreResponse.builder().id(1L).score(9).build();

        when(reviewService.submitScore(eq(1L), any(ReviewScoreRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/reviews/assignments/1/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").value(9));

        verify(reviewService, times(1)).submitScore(eq(1L), any(ReviewScoreRequest.class));
    }

    @Test
    void submit_Success() throws Exception {
        ReviewerAssignmentResponse response = ReviewerAssignmentResponse.builder().id(1L).status("SUBMITTED").build();

        when(reviewService.submitAssignment(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/reviews/assignments/1/submit")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        verify(reviewService, times(1)).submitAssignment(1L);
    }

    @Test
    void listAssignments_Success() throws Exception {
        ReviewerAssignmentResponse response = ReviewerAssignmentResponse.builder().id(1L).build();
        Page<ReviewerAssignmentResponse> page = new PageImpl<>(List.of(response));

        when(reviewService.searchAssignments(eq(1L), eq(2L), eq("ASSIGNED"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/reviews/assignments")
                        .param("applicationId", "1")
                        .param("reviewerId", "2")
                        .param("status", "ASSIGNED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(reviewService, times(1)).searchAssignments(eq(1L), eq(2L), eq("ASSIGNED"), any(Pageable.class));
    }

    @Test
    void listScores_Success() throws Exception {
        ReviewScoreResponse response = ReviewScoreResponse.builder().id(1L).score(9).build();

        when(reviewService.getScores(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/reviews/assignments/1/scores")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].score").value(9));

        verify(reviewService, times(1)).getScores(1L);
    }

    @Test
    void createPanelDecision_Success() throws Exception {
        PanelDecisionRequest request = new PanelDecisionRequest(LocalDate.now(), new BigDecimal("8.5"), "FUNDED", new BigDecimal("10000"), "Cond", null);
        PanelDecisionResponse response = PanelDecisionResponse.builder().id(1L).awardDecision("FUNDED").build();

        when(reviewService.createPanelDecision(eq(1L), any(PanelDecisionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/reviews/applications/1/panel-decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.awardDecision").value("FUNDED"));

        verify(reviewService, times(1)).createPanelDecision(eq(1L), any(PanelDecisionRequest.class));
    }

    @Test
    void getPanelDecision_Success() throws Exception {
        PanelDecisionResponse response = PanelDecisionResponse.builder().id(1L).awardDecision("FUNDED").build();

        when(reviewService.getPanelDecision(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/reviews/applications/1/panel-decision")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.awardDecision").value("FUNDED"));

        verify(reviewService, times(1)).getPanelDecision(1L);
    }
}

