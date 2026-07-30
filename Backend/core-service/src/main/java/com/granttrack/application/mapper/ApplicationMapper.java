package com.granttrack.application.mapper;

import com.granttrack.application.dto.response.ApplicationBudgetResponse;
import com.granttrack.application.dto.response.CoInvestigatorResponse;
import com.granttrack.application.dto.response.GrantApplicationResponse;
import com.granttrack.application.entity.ApplicationBudget;
import com.granttrack.application.entity.CoInvestigator;
import com.granttrack.application.entity.GrantApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps application-module entities to response DTOs. */
@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "status", expression = "java(application.getStatus().name())")
    GrantApplicationResponse toResponse(GrantApplication application);

    @Mapping(target = "applicationId", source = "application.id")
    @Mapping(target = "role", expression = "java(coInvestigator.getRole().name())")
    @Mapping(target = "status", expression = "java(coInvestigator.getStatus().name())")
    CoInvestigatorResponse toResponse(CoInvestigator coInvestigator);

    @Mapping(target = "applicationId", source = "application.id")
    @Mapping(target = "budgetHead", expression = "java(budget.getBudgetHead().name())")
    ApplicationBudgetResponse toResponse(ApplicationBudget budget);
}
