package ch.fhnw.modeller.webservice.dto;

import ch.fhnw.modeller.webservice.ontology.reasoning.SWRLRule;
import lombok.Data;

@Data
public class SWRLDto {
    private String ttlContent;
    private SWRLRule rule;

    public SWRLDto(){}

}
