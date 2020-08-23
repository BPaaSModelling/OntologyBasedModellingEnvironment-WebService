package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class ArrowStructuresDto {

    private final List<String> heads;
    private final List<String> strokes;

}
