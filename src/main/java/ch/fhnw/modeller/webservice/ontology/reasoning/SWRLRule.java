package ch.fhnw.modeller.webservice.ontology.reasoning;

import lombok.Data;
import lombok.Getter;

@Data
public class SWRLRule {

    @Getter
    private String ruleName;
    @Getter
    private String ruleContent;

    public SWRLRule() {
    }

    public SWRLRule(String ruleName, String rule) {
        this.ruleName = ruleName;
        this.ruleContent = rule;
    }

}
