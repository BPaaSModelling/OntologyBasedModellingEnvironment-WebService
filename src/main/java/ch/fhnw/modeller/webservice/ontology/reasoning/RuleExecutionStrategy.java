package ch.fhnw.modeller.webservice.ontology.reasoning;

import org.semanticweb.owlapi.model.OWLOntology;

import java.util.List;

public interface RuleExecutionStrategy<T> {

    List<T> executeRules(OWLOntology ontology, Object inputRule);

}
