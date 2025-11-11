package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class ObjectRelationshipDto {
    String propertyLabel;
    String domainInstance;
    String rangeInstance;
}
