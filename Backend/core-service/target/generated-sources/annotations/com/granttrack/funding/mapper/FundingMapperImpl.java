package com.granttrack.funding.mapper;

import com.granttrack.funding.dto.response.FundingSchemeResponse;
import com.granttrack.funding.dto.response.GrantCallResponse;
import com.granttrack.funding.dto.response.InstitutionResponse;
import com.granttrack.funding.dto.response.SponsorResponse;
import com.granttrack.funding.entity.FundingScheme;
import com.granttrack.funding.entity.GrantCall;
import com.granttrack.funding.entity.Institution;
import com.granttrack.funding.entity.Sponsor;
import java.math.BigDecimal;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-04T17:06:15+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class FundingMapperImpl implements FundingMapper {

    @Override
    public SponsorResponse toResponse(Sponsor sponsor) {
        if ( sponsor == null ) {
            return null;
        }

        SponsorResponse.SponsorResponseBuilder sponsorResponse = SponsorResponse.builder();

        sponsorResponse.address( sponsor.getAddress() );
        sponsorResponse.contactEmail( sponsor.getContactEmail() );
        sponsorResponse.id( sponsor.getId() );
        sponsorResponse.name( sponsor.getName() );
        sponsorResponse.phone( sponsor.getPhone() );
        sponsorResponse.sponsorCode( sponsor.getSponsorCode() );
        sponsorResponse.type( sponsor.getType() );
        sponsorResponse.website( sponsor.getWebsite() );

        return sponsorResponse.build();
    }

    @Override
    public InstitutionResponse toResponse(Institution institution) {
        if ( institution == null ) {
            return null;
        }

        InstitutionResponse.InstitutionResponseBuilder institutionResponse = InstitutionResponse.builder();

        institutionResponse.address( institution.getAddress() );
        institutionResponse.city( institution.getCity() );
        institutionResponse.country( institution.getCountry() );
        institutionResponse.createdAt( institution.getCreatedAt() );
        institutionResponse.email( institution.getEmail() );
        institutionResponse.id( institution.getId() );
        institutionResponse.institutionCode( institution.getInstitutionCode() );
        institutionResponse.mobileNumber( institution.getMobileNumber() );
        institutionResponse.name( institution.getName() );
        institutionResponse.pincode( institution.getPincode() );
        institutionResponse.state( institution.getState() );
        institutionResponse.type( institution.getType() );
        institutionResponse.universityName( institution.getUniversityName() );

        return institutionResponse.build();
    }

    @Override
    public FundingSchemeResponse toResponse(FundingScheme scheme) {
        if ( scheme == null ) {
            return null;
        }

        FundingSchemeResponse.FundingSchemeResponseBuilder fundingSchemeResponse = FundingSchemeResponse.builder();

        fundingSchemeResponse.sponsorId( schemeSponsorId( scheme ) );
        fundingSchemeResponse.sponsorName( schemeSponsorName( scheme ) );
        fundingSchemeResponse.category( scheme.getCategory() );
        fundingSchemeResponse.createdAt( scheme.getCreatedAt() );
        fundingSchemeResponse.description( scheme.getDescription() );
        fundingSchemeResponse.documentPath( scheme.getDocumentPath() );
        fundingSchemeResponse.eligibleApplicants( scheme.getEligibleApplicants() );
        fundingSchemeResponse.fromDate( scheme.getFromDate() );
        fundingSchemeResponse.fundingDurationMonths( scheme.getFundingDurationMonths() );
        fundingSchemeResponse.id( scheme.getId() );
        fundingSchemeResponse.maxAwardAmount( scheme.getMaxAwardAmount() );
        fundingSchemeResponse.minAwardAmount( scheme.getMinAwardAmount() );
        fundingSchemeResponse.researchArea( scheme.getResearchArea() );
        fundingSchemeResponse.schemeCode( scheme.getSchemeCode() );
        fundingSchemeResponse.schemeName( scheme.getSchemeName() );
        fundingSchemeResponse.toDate( scheme.getToDate() );
        fundingSchemeResponse.updatedAt( scheme.getUpdatedAt() );

        fundingSchemeResponse.status( scheme.getStatus().name() );

        return fundingSchemeResponse.build();
    }

    @Override
    public GrantCallResponse toResponse(GrantCall call) {
        if ( call == null ) {
            return null;
        }

        GrantCallResponse.GrantCallResponseBuilder grantCallResponse = GrantCallResponse.builder();

        grantCallResponse.schemeId( callSchemeId( call ) );
        grantCallResponse.schemeName( callSchemeSchemeName( call ) );
        grantCallResponse.schemeCategory( callSchemeCategory( call ) );
        grantCallResponse.eligibleApplicants( callSchemeEligibleApplicants( call ) );
        grantCallResponse.fundingDurationMonths( callSchemeFundingDurationMonths( call ) );
        grantCallResponse.schemeDocumentPath( callSchemeDocumentPath( call ) );
        grantCallResponse.schemeMaxAwardAmount( callSchemeMaxAwardAmount( call ) );
        grantCallResponse.callTitle( call.getCallTitle() );
        grantCallResponse.closeDate( call.getCloseDate() );
        grantCallResponse.createdAt( call.getCreatedAt() );
        grantCallResponse.expectedAwards( call.getExpectedAwards() );
        grantCallResponse.id( call.getId() );
        grantCallResponse.openDate( call.getOpenDate() );
        grantCallResponse.totalBudgetAllocated( call.getTotalBudgetAllocated() );
        grantCallResponse.updatedAt( call.getUpdatedAt() );

        grantCallResponse.reviewMethod( call.getReviewMethod().name() );
        grantCallResponse.status( call.getStatus().name() );

        return grantCallResponse.build();
    }

    private Long schemeSponsorId(FundingScheme fundingScheme) {
        Sponsor sponsor = fundingScheme.getSponsor();
        if ( sponsor == null ) {
            return null;
        }
        return sponsor.getId();
    }

    private String schemeSponsorName(FundingScheme fundingScheme) {
        Sponsor sponsor = fundingScheme.getSponsor();
        if ( sponsor == null ) {
            return null;
        }
        return sponsor.getName();
    }

    private Long callSchemeId(GrantCall grantCall) {
        FundingScheme scheme = grantCall.getScheme();
        if ( scheme == null ) {
            return null;
        }
        return scheme.getId();
    }

    private String callSchemeSchemeName(GrantCall grantCall) {
        FundingScheme scheme = grantCall.getScheme();
        if ( scheme == null ) {
            return null;
        }
        return scheme.getSchemeName();
    }

    private String callSchemeCategory(GrantCall grantCall) {
        FundingScheme scheme = grantCall.getScheme();
        if ( scheme == null ) {
            return null;
        }
        return scheme.getCategory();
    }

    private String callSchemeEligibleApplicants(GrantCall grantCall) {
        FundingScheme scheme = grantCall.getScheme();
        if ( scheme == null ) {
            return null;
        }
        return scheme.getEligibleApplicants();
    }

    private Integer callSchemeFundingDurationMonths(GrantCall grantCall) {
        FundingScheme scheme = grantCall.getScheme();
        if ( scheme == null ) {
            return null;
        }
        return scheme.getFundingDurationMonths();
    }

    private String callSchemeDocumentPath(GrantCall grantCall) {
        FundingScheme scheme = grantCall.getScheme();
        if ( scheme == null ) {
            return null;
        }
        return scheme.getDocumentPath();
    }

    private BigDecimal callSchemeMaxAwardAmount(GrantCall grantCall) {
        FundingScheme scheme = grantCall.getScheme();
        if ( scheme == null ) {
            return null;
        }
        return scheme.getMaxAwardAmount();
    }
}
