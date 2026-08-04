package com.granttrack.disbursement.mapper;

import com.granttrack.disbursement.dto.response.FundDisbursementResponse;
import com.granttrack.disbursement.dto.response.MilestoneResponse;
import com.granttrack.disbursement.entity.DisbursementMilestone;
import com.granttrack.disbursement.entity.FundDisbursement;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-04T17:06:22+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class DisbursementMapperImpl implements DisbursementMapper {

    @Override
    public MilestoneResponse toResponse(DisbursementMilestone milestone, String displayStatus) {
        if ( milestone == null && displayStatus == null ) {
            return null;
        }

        MilestoneResponse.MilestoneResponseBuilder milestoneResponse = MilestoneResponse.builder();

        if ( milestone != null ) {
            milestoneResponse.amount( milestone.getAmount() );
            milestoneResponse.awardId( milestone.getAwardId() );
            milestoneResponse.createdAt( milestone.getCreatedAt() );
            milestoneResponse.description( milestone.getDescription() );
            milestoneResponse.dueDate( milestone.getDueDate() );
            milestoneResponse.evidenceDocName( milestone.getEvidenceDocName() );
            milestoneResponse.evidenceNote( milestone.getEvidenceNote() );
            milestoneResponse.evidenceRequired( milestone.getEvidenceRequired() );
            milestoneResponse.evidenceReviewComment( milestone.getEvidenceReviewComment() );
            milestoneResponse.evidenceSubmittedDate( milestone.getEvidenceSubmittedDate() );
            milestoneResponse.id( milestone.getId() );
            milestoneResponse.milestoneNumber( milestone.getMilestoneNumber() );
            milestoneResponse.updatedAt( milestone.getUpdatedAt() );
        }
        milestoneResponse.status( displayStatus );
        milestoneResponse.hasEvidenceDocument( milestone.getEvidenceDocPath() != null );

        return milestoneResponse.build();
    }

    @Override
    public FundDisbursementResponse toResponse(FundDisbursement disbursement) {
        if ( disbursement == null ) {
            return null;
        }

        FundDisbursementResponse.FundDisbursementResponseBuilder fundDisbursementResponse = FundDisbursementResponse.builder();

        fundDisbursementResponse.milestoneId( disbursementMilestoneId( disbursement ) );
        fundDisbursementResponse.milestoneDescription( disbursementMilestoneDescription( disbursement ) );
        fundDisbursementResponse.amount( disbursement.getAmount() );
        fundDisbursementResponse.awardId( disbursement.getAwardId() );
        fundDisbursementResponse.createdAt( disbursement.getCreatedAt() );
        fundDisbursementResponse.disbursedDate( disbursement.getDisbursedDate() );
        fundDisbursementResponse.id( disbursement.getId() );
        fundDisbursementResponse.paymentReference( disbursement.getPaymentReference() );
        fundDisbursementResponse.receivingAccountRef( disbursement.getReceivingAccountRef() );
        fundDisbursementResponse.updatedAt( disbursement.getUpdatedAt() );

        fundDisbursementResponse.status( disbursement.getStatus().name() );

        return fundDisbursementResponse.build();
    }

    private Long disbursementMilestoneId(FundDisbursement fundDisbursement) {
        DisbursementMilestone milestone = fundDisbursement.getMilestone();
        if ( milestone == null ) {
            return null;
        }
        return milestone.getId();
    }

    private String disbursementMilestoneDescription(FundDisbursement fundDisbursement) {
        DisbursementMilestone milestone = fundDisbursement.getMilestone();
        if ( milestone == null ) {
            return null;
        }
        return milestone.getDescription();
    }
}
