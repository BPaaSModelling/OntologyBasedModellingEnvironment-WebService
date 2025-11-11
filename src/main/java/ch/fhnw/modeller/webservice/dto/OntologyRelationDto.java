package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class OntologyRelationDto {
    private String relationId;
    private String iri;
    private String label;
    private String domain;
    private String range;
}
