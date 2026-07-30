package com.granttrack.progress.mapper;

import com.granttrack.progress.dto.response.DeliverableResponse;
import com.granttrack.progress.dto.response.ProgressReportResponse;
import com.granttrack.progress.entity.Deliverable;
import com.granttrack.progress.entity.ProgressReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps progress-module entities to response DTOs. */
@Mapper(componentModel = "spring")
public interface ProgressMapper {

    @Mapping(target = "status", expression = "java(report.getStatus().name())")
    @Mapping(target = "hasReportDocument", expression = "java(report.getReportDocPath() != null)")
    ProgressReportResponse toResponse(ProgressReport report);

    @Mapping(target = "type", expression = "java(deliverable.getType().name())")
    @Mapping(target = "status", expression = "java(deliverable.getStatus().name())")
    @Mapping(target = "hasFile", expression = "java(deliverable.getFilePath() != null)")
    DeliverableResponse toResponse(Deliverable deliverable);
}
