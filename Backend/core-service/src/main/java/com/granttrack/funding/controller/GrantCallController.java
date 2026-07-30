package com.granttrack.funding.controller;

import com.granttrack.common.dto.ApiResponse;
import com.granttrack.common.dto.PageResponse;
import com.granttrack.funding.dto.request.GrantCallRequest;
import com.granttrack.funding.dto.response.GrantCallResponse;
import com.granttrack.funding.service.GrantCallService;
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
@RequestMapping("/api/v1/funding/calls")
@RequiredArgsConstructor
@Tag(name = "Funding — Grant Calls", description = "Open and manage grant calls / submission windows")
public class GrantCallController {

    private final GrantCallService callService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Create a grant call")
    public ResponseEntity<ApiResponse<GrantCallResponse>> create(@Valid @RequestBody GrantCallRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Grant call created", callService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Update a grant call")
    public ResponseEntity<ApiResponse<GrantCallResponse>> update(@PathVariable Long id,
                                                                 @Valid @RequestBody GrantCallRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Grant call updated", callService.update(id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a grant call by id")
    public ResponseEntity<ApiResponse<GrantCallResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(callService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Search grant calls (filter by status / scheme, paginated)")
    public ResponseEntity<ApiResponse<PageResponse<GrantCallResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long schemeId,
            @PageableDefault(size = 20, sort = "openDate") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(callService.search(q, status, schemeId, pageable))));
    }

    @PostMapping("/{id}/open")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Open a grant call for submissions")
    public ResponseEntity<ApiResponse<GrantCallResponse>> open(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Grant call opened", callService.open(id)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Close a grant call")
    public ResponseEntity<ApiResponse<GrantCallResponse>> close(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Grant call closed", callService.close(id)));
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Terminate a grant call")
    public ResponseEntity<ApiResponse<GrantCallResponse>> terminate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Grant call terminated", callService.terminate(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Soft-delete a grant call")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        callService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Grant call deleted"));
    }
}
