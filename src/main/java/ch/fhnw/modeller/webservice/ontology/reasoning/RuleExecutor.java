package ch.fhnw.modeller.webservice.ontology.reasoning;

import lombok.Setter;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.List;

@Setter
public class RuleExecutor<T> {

    private RuleExecutionStrategy<T> strategy;

    public RuleExecutor(RuleExecutionStrategy<T> strategy) {
        this.strategy = strategy;
    }

    public List<T> execute(OWLOntology ontology, Object ruleInput) {
        return strategy.executeRules(ontology, ruleInput);
    }
}
