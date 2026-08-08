package com.granttrack.output.mapper;

import com.granttrack.output.dto.response.IPRecordResponse;
import com.granttrack.output.dto.response.ResearchOutputResponse;
import com.granttrack.output.entity.IPRecord;
import com.granttrack.output.entity.ResearchOutput;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-08T18:32:52+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class OutputMapperImpl implements OutputMapper {

    @Override
    public ResearchOutputResponse toResponse(ResearchOutput output) {
        if ( output == null ) {
            return null;
        }

        ResearchOutputResponse.ResearchOutputResponseBuilder researchOutputResponse = ResearchOutputResponse.builder();

        researchOutputResponse.authors( output.getAuthors() );
        researchOutputResponse.awardId( output.getAwardId() );
        researchOutputResponse.createdAt( output.getCreatedAt() );
        researchOutputResponse.doi( output.getDoi() );
        researchOutputResponse.id( output.getId() );
        researchOutputResponse.openAccessCompliant( output.getOpenAccessCompliant() );
        researchOutputResponse.publicationVenue( output.getPublicationVenue() );
        researchOutputResponse.publishedDate( output.getPublishedDate() );
        researchOutputResponse.title( output.getTitle() );
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

        iPRecordResponse.awardId( record.getAwardId() );
        iPRecordResponse.createdAt( record.getCreatedAt() );
        iPRecordResponse.filingDate( record.getFilingDate() );
        iPRecordResponse.grantDate( record.getGrantDate() );
        iPRecordResponse.id( record.getId() );
        iPRecordResponse.inventors( record.getInventors() );
        iPRecordResponse.ownershipPercent( record.getOwnershipPercent() );
        iPRecordResponse.title( record.getTitle() );
        iPRecordResponse.updatedAt( record.getUpdatedAt() );

        iPRecordResponse.ipType( record.getIpType().name() );
        iPRecordResponse.status( record.getStatus().name() );

        return iPRecordResponse.build();
    }
}
