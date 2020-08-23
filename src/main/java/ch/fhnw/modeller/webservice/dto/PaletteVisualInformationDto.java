package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

import java.util.Map;

@Data
public class PaletteVisualInformationDto {

    private String imageUrl;
    private String fromArrow;
    private String toArrow;
    private String arrowStroke;
}
