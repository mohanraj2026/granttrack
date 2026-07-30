package com.granttrack.award.mapper;

import com.granttrack.award.dto.response.GrantAwardResponse;
import com.granttrack.award.entity.GrantAward;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps award-module entities to response DTOs. */
@Mapper(componentModel = "spring")
public interface AwardMapper {

    @Mapping(target = "status", expression = "java(award.getStatus().name())")
    @Mapping(target = "financeReviewStatus",
            expression = "java(award.getFinanceReviewStatus() == null ? null : award.getFinanceReviewStatus().name())")
    GrantAwardResponse toResponse(GrantAward award);
}
