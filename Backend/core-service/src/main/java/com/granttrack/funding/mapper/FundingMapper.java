package com.granttrack.funding.mapper;

import com.granttrack.funding.dto.response.FundingSchemeResponse;
import com.granttrack.funding.dto.response.GrantCallResponse;
import com.granttrack.funding.dto.response.InstitutionResponse;
import com.granttrack.funding.dto.response.SponsorResponse;
import com.granttrack.funding.entity.FundingScheme;
import com.granttrack.funding.entity.GrantCall;
import com.granttrack.funding.entity.Institution;
import com.granttrack.funding.entity.Sponsor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps funding-module entities to response DTOs. */
@Mapper(componentModel = "spring")
public interface FundingMapper {

    SponsorResponse toResponse(Sponsor sponsor);

    InstitutionResponse toResponse(Institution institution);

    @Mapping(target = "sponsorId", source = "sponsor.id")
    @Mapping(target = "sponsorName", source = "sponsor.name")
    @Mapping(target = "status", expression = "java(scheme.getStatus().name())")
    FundingSchemeResponse toResponse(FundingScheme scheme);

    @Mapping(target = "schemeId", source = "scheme.id")
    @Mapping(target = "schemeName", source = "scheme.schemeName")
    @Mapping(target = "schemeCategory", source = "scheme.category")
    @Mapping(target = "eligibleApplicants", source = "scheme.eligibleApplicants")
    @Mapping(target = "fundingDurationMonths", source = "scheme.fundingDurationMonths")
    @Mapping(target = "schemeDocumentPath", source = "scheme.documentPath")
    @Mapping(target = "schemeMaxAwardAmount", source = "scheme.maxAwardAmount")
    @Mapping(target = "reviewMethod", expression = "java(call.getReviewMethod().name())")
    @Mapping(target = "status", expression = "java(call.getStatus().name())")
    GrantCallResponse toResponse(GrantCall call);
}
