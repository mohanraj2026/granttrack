package com.granttrack.progress.mapper;

import com.granttrack.progress.dto.response.DeliverableResponse;
import com.granttrack.progress.dto.response.ProgressReportResponse;
import com.granttrack.progress.entity.Deliverable;
import com.granttrack.progress.entity.ProgressReport;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-08T18:32:52+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class ProgressMapperImpl implements ProgressMapper {

    @Override
    public ProgressReportResponse toResponse(ProgressReport report) {
        if ( report == null ) {
            return null;
        }

        ProgressReportResponse.ProgressReportResponseBuilder progressReportResponse = ProgressReportResponse.builder();

        progressReportResponse.awardId( report.getAwardId() );
        progressReportResponse.budgetUtilisationPercent( report.getBudgetUtilisationPercent() );
        progressReportResponse.challenges( report.getChallenges() );
        progressReportResponse.createdAt( report.getCreatedAt() );
        progressReportResponse.id( report.getId() );
        progressReportResponse.keyAchievements( report.getKeyAchievements() );
        progressReportResponse.milestoneId( report.getMilestoneId() );
        progressReportResponse.period( report.getPeriod() );
        progressReportResponse.reportDocName( report.getReportDocName() );
        progressReportResponse.reviewComment( report.getReviewComment() );
        progressReportResponse.submittedById( report.getSubmittedById() );
        progressReportResponse.submittedDate( report.getSubmittedDate() );
        progressReportResponse.summary( report.getSummary() );
        progressReportResponse.updatedAt( report.getUpdatedAt() );

        progressReportResponse.status( report.getStatus().name() );
        progressReportResponse.hasReportDocument( report.getReportDocPath() != null );

        return progressReportResponse.build();
    }

    @Override
    public DeliverableResponse toResponse(Deliverable deliverable) {
        if ( deliverable == null ) {
            return null;
        }

        DeliverableResponse.DeliverableResponseBuilder deliverableResponse = DeliverableResponse.builder();

        deliverableResponse.awardId( deliverable.getAwardId() );
        deliverableResponse.createdAt( deliverable.getCreatedAt() );
        deliverableResponse.dueDate( deliverable.getDueDate() );
        deliverableResponse.fileName( deliverable.getFileName() );
        deliverableResponse.filePath( deliverable.getFilePath() );
        deliverableResponse.id( deliverable.getId() );
        deliverableResponse.reviewComment( deliverable.getReviewComment() );
        deliverableResponse.submittedDate( deliverable.getSubmittedDate() );
        deliverableResponse.title( deliverable.getTitle() );
        deliverableResponse.updatedAt( deliverable.getUpdatedAt() );

        deliverableResponse.type( deliverable.getType().name() );
        deliverableResponse.status( deliverable.getStatus().name() );
        deliverableResponse.hasFile( deliverable.getFilePath() != null );

        return deliverableResponse.build();
    }
}
