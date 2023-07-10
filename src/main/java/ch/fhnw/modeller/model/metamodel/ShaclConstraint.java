package ch.fhnw.modeller.model.metamodel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShaclConstraint {

    private String id;
    private String domainName;
    private String name;
    private String description;
    private String path;
    private String minCount;
    private String maxCount;

    private String label;
    private String range;
    private String defaultValue;


}
