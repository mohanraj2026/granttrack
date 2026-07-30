package com.granttrack.output.controller;

import com.granttrack.common.dto.ApiResponse;
import com.granttrack.common.dto.PageResponse;
import com.granttrack.output.dto.request.IPRecordRequest;
import com.granttrack.output.dto.response.IPRecordResponse;
import com.granttrack.output.service.IPRecordService;
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
@RequestMapping("/api/v1/outputs/ip")
@RequiredArgsConstructor
@Tag(name = "Outputs — IP Records", description = "Manage intellectual-property records arising from awards")
public class IPRecordController {

    private final IPRecordService ipRecordService;

    @PostMapping
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Add an IP record / patent")
    public ResponseEntity<ApiResponse<IPRecordResponse>> create(@Valid @RequestBody IPRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("IP record created", ipRecordService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESEARCHER')")
    @Operation(summary = "Update an IP record")
    public ResponseEntity<ApiResponse<IPRecordResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody IPRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.success("IP record updated", ipRecordService.update(id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an IP record by id")
    public ResponseEntity<ApiResponse<IPRecordResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(ipRecordService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List IP records (filter by awardId, status; paginated)")
    public ResponseEntity<ApiResponse<PageResponse<IPRecordResponse>>> list(
            @RequestParam(required = false) Long awardId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(ipRecordService.list(awardId, status, pageable))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESEARCHER','ADMIN')")
    @Operation(summary = "Soft-delete an IP record")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        ipRecordService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("IP record deleted"));
    }
}
