package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class DatatypeAssertionDto {
    public String propertyUri;
    public String domainInstanceUri;
    public String value;
    public String datatypeUri;
    public String lang;
}
