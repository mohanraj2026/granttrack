package com.granttrack.award.mapper;

import com.granttrack.award.dto.response.GrantAwardResponse;
import com.granttrack.award.entity.GrantAward;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-30T16:08:59+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class AwardMapperImpl implements AwardMapper {

    @Override
    public GrantAwardResponse toResponse(GrantAward award) {
        if ( award == null ) {
            return null;
        }

        GrantAwardResponse.GrantAwardResponseBuilder grantAwardResponse = GrantAwardResponse.builder();

        grantAwardResponse.applicationId( award.getApplicationId() );
        grantAwardResponse.awardLetterDate( award.getAwardLetterDate() );
        grantAwardResponse.awardedAmount( award.getAwardedAmount() );
        grantAwardResponse.conditionsRef( award.getConditionsRef() );
        grantAwardResponse.createdAt( award.getCreatedAt() );
        grantAwardResponse.endDate( award.getEndDate() );
        grantAwardResponse.financeOfficerId( award.getFinanceOfficerId() );
        grantAwardResponse.financeReviewComment( award.getFinanceReviewComment() );
        grantAwardResponse.id( award.getId() );
        grantAwardResponse.startDate( award.getStartDate() );
        grantAwardResponse.updatedAt( award.getUpdatedAt() );

        grantAwardResponse.status( award.getStatus().name() );
        grantAwardResponse.financeReviewStatus( award.getFinanceReviewStatus() == null ? null : award.getFinanceReviewStatus().name() );

        return grantAwardResponse.build();
    }
}
