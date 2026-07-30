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
    date = "2026-07-30T16:08:59+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class ApplicationMapperImpl implements ApplicationMapper {

    @Override
    public GrantApplicationResponse toResponse(GrantApplication application) {
        if ( application == null ) {
            return null;
        }

        GrantApplicationResponse.GrantApplicationResponseBuilder grantApplicationResponse = GrantApplicationResponse.builder();

        grantApplicationResponse.abstractDocName( application.getAbstractDocName() );
        grantApplicationResponse.abstractDocPath( application.getAbstractDocPath() );
        grantApplicationResponse.callId( application.getCallId() );
        grantApplicationResponse.createdAt( application.getCreatedAt() );
        grantApplicationResponse.discipline( application.getDiscipline() );
        grantApplicationResponse.id( application.getId() );
        grantApplicationResponse.institutionId( application.getInstitutionId() );
        grantApplicationResponse.principalInvestigatorId( application.getPrincipalInvestigatorId() );
        grantApplicationResponse.projectDurationMonths( application.getProjectDurationMonths() );
        grantApplicationResponse.projectTitle( application.getProjectTitle() );
        grantApplicationResponse.requestedAmount( application.getRequestedAmount() );
        grantApplicationResponse.researchAbstract( application.getResearchAbstract() );
        grantApplicationResponse.submissionDate( application.getSubmissionDate() );
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
        coInvestigatorResponse.contribution( coInvestigator.getContribution() );
        coInvestigatorResponse.createdAt( coInvestigator.getCreatedAt() );
        coInvestigatorResponse.id( coInvestigator.getId() );
        coInvestigatorResponse.institutionId( coInvestigator.getInstitutionId() );
        coInvestigatorResponse.updatedAt( coInvestigator.getUpdatedAt() );
        coInvestigatorResponse.userId( coInvestigator.getUserId() );

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
        applicationBudgetResponse.amount( budget.getAmount() );
        applicationBudgetResponse.createdAt( budget.getCreatedAt() );
        applicationBudgetResponse.id( budget.getId() );
        applicationBudgetResponse.justification( budget.getJustification() );
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
