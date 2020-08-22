package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class DiagramCreationDto {

    private String paletteConstruct;
    private int x;
    private int y;
    private String uuid;
    private String label;
}
