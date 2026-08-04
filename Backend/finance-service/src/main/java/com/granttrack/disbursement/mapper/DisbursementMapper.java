package com.granttrack.disbursement.mapper;

import com.granttrack.disbursement.dto.response.FundDisbursementResponse;
import com.granttrack.disbursement.dto.response.MilestoneResponse;
import com.granttrack.disbursement.entity.DisbursementMilestone;
import com.granttrack.disbursement.entity.FundDisbursement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps disbursement-module entities to response DTOs. */
@Mapper(componentModel = "spring")
public interface DisbursementMapper {

    @Mapping(target = "status", source = "displayStatus")
    @Mapping(target = "hasEvidenceDocument", expression = "java(milestone.getEvidenceDocPath() != null)")
    MilestoneResponse toResponse(DisbursementMilestone milestone, String displayStatus);

    @Mapping(target = "milestoneId", source = "milestone.id")
    @Mapping(target = "milestoneDescription", source = "milestone.description")
    @Mapping(target = "status", expression = "java(disbursement.getStatus().name())")
    FundDisbursementResponse toResponse(FundDisbursement disbursement);
}
