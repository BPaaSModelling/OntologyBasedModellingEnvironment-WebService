package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class ModelElementAttributes {

    private final Set<RelationDto> options;
    private final List<ModelElementAttribute> values;

}
