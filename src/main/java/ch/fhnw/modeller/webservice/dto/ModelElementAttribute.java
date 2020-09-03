package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class ModelElementAttribute {

    private String relationPrefix;
    private String relation;
    private String valuePrefix;
    private String value;
}
