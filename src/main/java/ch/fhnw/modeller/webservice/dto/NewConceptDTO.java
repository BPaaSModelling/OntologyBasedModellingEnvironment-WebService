package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class NewConceptDTO {
    private String uuid;
    private String parentLanguageClass;
    private String label;
    private String representedLanguageClass;
    private String comment;
}
