package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class ModelElementCreationDto {

    private String paletteConstruct;
    private int x;
    private int y;
    private int w;
    private int h;
    private String uuid;
    private String label;
    private String modelingLanguageConstructInstance;
    private InstantiationTargetType instantiationType;
    private String note;
    private String shapeRepresentsModel;
}
