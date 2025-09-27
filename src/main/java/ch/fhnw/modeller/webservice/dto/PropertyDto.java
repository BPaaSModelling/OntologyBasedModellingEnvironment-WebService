package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class PropertyDto {
    String label;
    String domainClassUri;
    String xsdRange;
}
