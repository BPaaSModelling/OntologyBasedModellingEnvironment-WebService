package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class NewAttributeDto {
    private String label;
    private String DomainClassURI;
    private String selectedInstance;
    private String range;
    private String value;
}
