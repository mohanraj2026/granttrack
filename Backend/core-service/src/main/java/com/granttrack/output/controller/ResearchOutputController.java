package com.granttrack.output.controller;

import com.granttrack.common.dto.ApiResponse;
import com.granttrack.common.dto.PageResponse;
import com.granttrack.output.dto.request.ResearchOutputRequest;
import com.granttrack.output.dto.response.ResearchOutputResponse;
import com.granttrack.output.service.ResearchOutputService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outputs")
@RequiredArgsConstructor
@Tag(name = "Outputs — Research Outputs", description = "Manage research outputs produced under awards")
public class ResearchOutputController {

    private final ResearchOutputService outputService;

    @PostMapping
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Add a research output")
    public ResponseEntity<ApiResponse<ResearchOutputResponse>> create(@Valid @RequestBody ResearchOutputRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Research output created", outputService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Update a research output")
    public ResponseEntity<ApiResponse<ResearchOutputResponse>> update(@PathVariable Long id,
                                                                      @Valid @RequestBody ResearchOutputRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Research output updated", outputService.update(id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a research output by id")
    public ResponseEntity<ApiResponse<ResearchOutputResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(outputService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Search research outputs (filter by awardId, type, status, q on title; paginated)")
    public ResponseEntity<ApiResponse<PageResponse<ResearchOutputResponse>>> search(
            @RequestParam(required = false) Long awardId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(outputService.search(awardId, type, status, q, pageable))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESEARCHER','ADMIN')")
    @Operation(summary = "Soft-delete a research output")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        outputService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Research output deleted"));
    }
}
