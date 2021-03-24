package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

import java.util.Set;

@Data
public class Options {

    private final Set<String> instances;
    private final Set<String> classes;
    private final boolean isPrimitive;
    private final String primitiveTypeRange;
}
