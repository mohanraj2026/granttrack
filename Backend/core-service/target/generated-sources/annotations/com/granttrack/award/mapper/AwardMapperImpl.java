package com.granttrack.award.mapper;

import com.granttrack.award.dto.response.GrantAwardResponse;
import com.granttrack.award.entity.GrantAward;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-06T11:07:41+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.2 (Oracle Corporation)"
)
@Component
public class AwardMapperImpl implements AwardMapper {

    @Override
    public GrantAwardResponse toResponse(GrantAward award) {
        if ( award == null ) {
            return null;
        }

        GrantAwardResponse.GrantAwardResponseBuilder grantAwardResponse = GrantAwardResponse.builder();

        grantAwardResponse.id( award.getId() );
        grantAwardResponse.applicationId( award.getApplicationId() );
        grantAwardResponse.awardedAmount( award.getAwardedAmount() );
        grantAwardResponse.startDate( award.getStartDate() );
        grantAwardResponse.endDate( award.getEndDate() );
        grantAwardResponse.conditionsRef( award.getConditionsRef() );
        grantAwardResponse.awardLetterDate( award.getAwardLetterDate() );
        grantAwardResponse.financeOfficerId( award.getFinanceOfficerId() );
        grantAwardResponse.financeReviewComment( award.getFinanceReviewComment() );
        grantAwardResponse.createdAt( award.getCreatedAt() );
        grantAwardResponse.updatedAt( award.getUpdatedAt() );

        grantAwardResponse.status( award.getStatus().name() );
        grantAwardResponse.financeReviewStatus( award.getFinanceReviewStatus() == null ? null : award.getFinanceReviewStatus().name() );

        return grantAwardResponse.build();
    }
}
