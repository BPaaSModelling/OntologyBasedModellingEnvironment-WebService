package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class ConnectionCreationDto {

    private String paletteConstruct;
    private int x;
    private int y;
    private String uuid;
    private String to;
    private String from;
    private InstantiationTargetType instantiationType;
}
