package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class ModelElementType {
    private String type;
    private InstantiationTargetType instantiationType;
    private String modellingLanguageConstruct;
}
