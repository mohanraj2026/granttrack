package com.granttrack.disbursement.mapper;

import com.granttrack.disbursement.dto.response.FundDisbursementResponse;
import com.granttrack.disbursement.dto.response.MilestoneResponse;
import com.granttrack.disbursement.entity.DisbursementMilestone;
import com.granttrack.disbursement.entity.FundDisbursement;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-30T11:27:25+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.2 (Oracle Corporation)"
)
@Component
public class DisbursementMapperImpl implements DisbursementMapper {

    @Override
    public MilestoneResponse toResponse(DisbursementMilestone milestone) {
        if ( milestone == null ) {
            return null;
        }

        MilestoneResponse.MilestoneResponseBuilder milestoneResponse = MilestoneResponse.builder();

        milestoneResponse.id( milestone.getId() );
        milestoneResponse.awardId( milestone.getAwardId() );
        milestoneResponse.milestoneNumber( milestone.getMilestoneNumber() );
        milestoneResponse.description( milestone.getDescription() );
        milestoneResponse.dueDate( milestone.getDueDate() );
        milestoneResponse.amount( milestone.getAmount() );
        milestoneResponse.evidenceRequired( milestone.getEvidenceRequired() );
        milestoneResponse.evidenceNote( milestone.getEvidenceNote() );
        milestoneResponse.evidenceDocName( milestone.getEvidenceDocName() );
        milestoneResponse.evidenceSubmittedDate( milestone.getEvidenceSubmittedDate() );
        milestoneResponse.evidenceReviewComment( milestone.getEvidenceReviewComment() );
        milestoneResponse.createdAt( milestone.getCreatedAt() );
        milestoneResponse.updatedAt( milestone.getUpdatedAt() );

        milestoneResponse.status( milestone.getStatus().name() );
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
        fundDisbursementResponse.id( disbursement.getId() );
        fundDisbursementResponse.awardId( disbursement.getAwardId() );
        fundDisbursementResponse.amount( disbursement.getAmount() );
        fundDisbursementResponse.disbursedDate( disbursement.getDisbursedDate() );
        fundDisbursementResponse.receivingAccountRef( disbursement.getReceivingAccountRef() );
        fundDisbursementResponse.paymentReference( disbursement.getPaymentReference() );
        fundDisbursementResponse.createdAt( disbursement.getCreatedAt() );
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
