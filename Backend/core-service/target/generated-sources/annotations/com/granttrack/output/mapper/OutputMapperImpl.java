package com.granttrack.output.mapper;

import com.granttrack.output.dto.response.IPRecordResponse;
import com.granttrack.output.dto.response.ResearchOutputResponse;
import com.granttrack.output.entity.IPRecord;
import com.granttrack.output.entity.ResearchOutput;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-06T11:07:41+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.2 (Oracle Corporation)"
)
@Component
public class OutputMapperImpl implements OutputMapper {

    @Override
    public ResearchOutputResponse toResponse(ResearchOutput output) {
        if ( output == null ) {
            return null;
        }

        ResearchOutputResponse.ResearchOutputResponseBuilder researchOutputResponse = ResearchOutputResponse.builder();

        researchOutputResponse.id( output.getId() );
        researchOutputResponse.awardId( output.getAwardId() );
        researchOutputResponse.title( output.getTitle() );
        researchOutputResponse.authors( output.getAuthors() );
        researchOutputResponse.publicationVenue( output.getPublicationVenue() );
        researchOutputResponse.doi( output.getDoi() );
        researchOutputResponse.publishedDate( output.getPublishedDate() );
        researchOutputResponse.openAccessCompliant( output.getOpenAccessCompliant() );
        researchOutputResponse.createdAt( output.getCreatedAt() );
        researchOutputResponse.updatedAt( output.getUpdatedAt() );

        researchOutputResponse.type( output.getType().name() );
        researchOutputResponse.status( output.getStatus().name() );

        return researchOutputResponse.build();
    }

    @Override
    public IPRecordResponse toResponse(IPRecord record) {
        if ( record == null ) {
            return null;
        }

        IPRecordResponse.IPRecordResponseBuilder iPRecordResponse = IPRecordResponse.builder();

        iPRecordResponse.id( record.getId() );
        iPRecordResponse.awardId( record.getAwardId() );
        iPRecordResponse.title( record.getTitle() );
        iPRecordResponse.inventors( record.getInventors() );
        iPRecordResponse.filingDate( record.getFilingDate() );
        iPRecordResponse.grantDate( record.getGrantDate() );
        iPRecordResponse.ownershipPercent( record.getOwnershipPercent() );
        iPRecordResponse.createdAt( record.getCreatedAt() );
        iPRecordResponse.updatedAt( record.getUpdatedAt() );

        iPRecordResponse.ipType( record.getIpType().name() );
        iPRecordResponse.status( record.getStatus().name() );

        return iPRecordResponse.build();
    }
}
