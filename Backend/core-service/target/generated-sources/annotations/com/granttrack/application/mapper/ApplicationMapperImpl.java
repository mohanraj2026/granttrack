package com.granttrack.application.mapper;

import com.granttrack.application.dto.response.ApplicationBudgetResponse;
import com.granttrack.application.dto.response.CoInvestigatorResponse;
import com.granttrack.application.dto.response.GrantApplicationResponse;
import com.granttrack.application.entity.ApplicationBudget;
import com.granttrack.application.entity.CoInvestigator;
import com.granttrack.application.entity.GrantApplication;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-06T11:07:41+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.2 (Oracle Corporation)"
)
@Component
public class ApplicationMapperImpl implements ApplicationMapper {

    @Override
    public GrantApplicationResponse toResponse(GrantApplication application) {
        if ( application == null ) {
            return null;
        }

        GrantApplicationResponse.GrantApplicationResponseBuilder grantApplicationResponse = GrantApplicationResponse.builder();

        grantApplicationResponse.id( application.getId() );
        grantApplicationResponse.callId( application.getCallId() );
        grantApplicationResponse.principalInvestigatorId( application.getPrincipalInvestigatorId() );
        grantApplicationResponse.projectTitle( application.getProjectTitle() );
        grantApplicationResponse.researchAbstract( application.getResearchAbstract() );
        grantApplicationResponse.discipline( application.getDiscipline() );
        grantApplicationResponse.requestedAmount( application.getRequestedAmount() );
        grantApplicationResponse.projectDurationMonths( application.getProjectDurationMonths() );
        grantApplicationResponse.institutionId( application.getInstitutionId() );
        grantApplicationResponse.submissionDate( application.getSubmissionDate() );
        grantApplicationResponse.abstractDocPath( application.getAbstractDocPath() );
        grantApplicationResponse.abstractDocName( application.getAbstractDocName() );
        grantApplicationResponse.createdAt( application.getCreatedAt() );
        grantApplicationResponse.updatedAt( application.getUpdatedAt() );

        grantApplicationResponse.status( application.getStatus().name() );

        return grantApplicationResponse.build();
    }

    @Override
    public CoInvestigatorResponse toResponse(CoInvestigator coInvestigator) {
        if ( coInvestigator == null ) {
            return null;
        }

        CoInvestigatorResponse.CoInvestigatorResponseBuilder coInvestigatorResponse = CoInvestigatorResponse.builder();

        coInvestigatorResponse.applicationId( coInvestigatorApplicationId( coInvestigator ) );
        coInvestigatorResponse.id( coInvestigator.getId() );
        coInvestigatorResponse.userId( coInvestigator.getUserId() );
        coInvestigatorResponse.institutionId( coInvestigator.getInstitutionId() );
        coInvestigatorResponse.contribution( coInvestigator.getContribution() );
        coInvestigatorResponse.createdAt( coInvestigator.getCreatedAt() );
        coInvestigatorResponse.updatedAt( coInvestigator.getUpdatedAt() );

        coInvestigatorResponse.role( coInvestigator.getRole().name() );
        coInvestigatorResponse.status( coInvestigator.getStatus().name() );

        return coInvestigatorResponse.build();
    }

    @Override
    public ApplicationBudgetResponse toResponse(ApplicationBudget budget) {
        if ( budget == null ) {
            return null;
        }

        ApplicationBudgetResponse.ApplicationBudgetResponseBuilder applicationBudgetResponse = ApplicationBudgetResponse.builder();

        applicationBudgetResponse.applicationId( budgetApplicationId( budget ) );
        applicationBudgetResponse.id( budget.getId() );
        applicationBudgetResponse.amount( budget.getAmount() );
        applicationBudgetResponse.justification( budget.getJustification() );
        applicationBudgetResponse.createdAt( budget.getCreatedAt() );
        applicationBudgetResponse.updatedAt( budget.getUpdatedAt() );

        applicationBudgetResponse.budgetHead( budget.getBudgetHead().name() );

        return applicationBudgetResponse.build();
    }

    private Long coInvestigatorApplicationId(CoInvestigator coInvestigator) {
        GrantApplication application = coInvestigator.getApplication();
        if ( application == null ) {
            return null;
        }
        return application.getId();
    }

    private Long budgetApplicationId(ApplicationBudget applicationBudget) {
        GrantApplication application = applicationBudget.getApplication();
        if ( application == null ) {
            return null;
        }
        return application.getId();
    }
}
