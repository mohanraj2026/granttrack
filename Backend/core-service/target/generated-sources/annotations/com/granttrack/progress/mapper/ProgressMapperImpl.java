package com.granttrack.progress.mapper;

import com.granttrack.progress.dto.response.DeliverableResponse;
import com.granttrack.progress.dto.response.ProgressReportResponse;
import com.granttrack.progress.entity.Deliverable;
import com.granttrack.progress.entity.ProgressReport;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-08T13:42:54+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.2 (Oracle Corporation)"
)
@Component
public class ProgressMapperImpl implements ProgressMapper {

    @Override
    public ProgressReportResponse toResponse(ProgressReport report) {
        if ( report == null ) {
            return null;
        }

        ProgressReportResponse.ProgressReportResponseBuilder progressReportResponse = ProgressReportResponse.builder();

        progressReportResponse.id( report.getId() );
        progressReportResponse.awardId( report.getAwardId() );
        progressReportResponse.milestoneId( report.getMilestoneId() );
        progressReportResponse.period( report.getPeriod() );
        progressReportResponse.summary( report.getSummary() );
        progressReportResponse.keyAchievements( report.getKeyAchievements() );
        progressReportResponse.challenges( report.getChallenges() );
        progressReportResponse.budgetUtilisationPercent( report.getBudgetUtilisationPercent() );
        progressReportResponse.submittedById( report.getSubmittedById() );
        progressReportResponse.submittedDate( report.getSubmittedDate() );
        progressReportResponse.reportDocName( report.getReportDocName() );
        progressReportResponse.reviewComment( report.getReviewComment() );
        progressReportResponse.createdAt( report.getCreatedAt() );
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

        deliverableResponse.id( deliverable.getId() );
        deliverableResponse.awardId( deliverable.getAwardId() );
        deliverableResponse.title( deliverable.getTitle() );
        deliverableResponse.dueDate( deliverable.getDueDate() );
        deliverableResponse.submittedDate( deliverable.getSubmittedDate() );
        deliverableResponse.filePath( deliverable.getFilePath() );
        deliverableResponse.fileName( deliverable.getFileName() );
        deliverableResponse.reviewComment( deliverable.getReviewComment() );
        deliverableResponse.createdAt( deliverable.getCreatedAt() );
        deliverableResponse.updatedAt( deliverable.getUpdatedAt() );

        deliverableResponse.type( deliverable.getType().name() );
        deliverableResponse.status( deliverable.getStatus().name() );
        deliverableResponse.hasFile( deliverable.getFilePath() != null );

        return deliverableResponse.build();
    }
}
