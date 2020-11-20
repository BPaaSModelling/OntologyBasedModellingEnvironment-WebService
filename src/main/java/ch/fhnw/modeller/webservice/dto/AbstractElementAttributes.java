package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Data
public class AbstractElementAttributes {

    private final String modellingLanguageConstruct;
    private final Set<RelationDto> options;
    private final List<ModelElementAttribute> values;
    private final String modelElementType;
    private final InstantiationTargetType instantiationType;
    private final List<String> referencingShapes;

    public Optional<String> getModelElementType() {
        return Optional.ofNullable(modelElementType);
    }

}
