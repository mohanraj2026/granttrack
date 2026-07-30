package com.granttrack.output.mapper;

import com.granttrack.output.dto.response.IPRecordResponse;
import com.granttrack.output.dto.response.ResearchOutputResponse;
import com.granttrack.output.entity.IPRecord;
import com.granttrack.output.entity.ResearchOutput;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps output-module entities to response DTOs. */
@Mapper(componentModel = "spring")
public interface OutputMapper {

    @Mapping(target = "type", expression = "java(output.getType().name())")
    @Mapping(target = "status", expression = "java(output.getStatus().name())")
    ResearchOutputResponse toResponse(ResearchOutput output);

    @Mapping(target = "ipType", expression = "java(record.getIpType().name())")
    @Mapping(target = "status", expression = "java(record.getStatus().name())")
    IPRecordResponse toResponse(IPRecord record);
}
