package ch.fhnw.modeller.webservice.ontology.reasoning;

import ch.fhnw.modeller.webservice.dto.SWRLResponseDto;
import org.semanticweb.owlapi.model.*;
import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.factory.SWRLAPIFactory;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SWRLExecutor implements RuleExecutionStrategy<SWRLResponseDto> {

    public static final Logger logger = Logger.getLogger(SWRLExecutor.class.getName());

    @Override
    public List<SWRLResponseDto> executeRules(OWLOntology ontology, Object ruleInput) {
        if (!(ruleInput instanceof SWRLRule)) {
            throw new IllegalArgumentException("SWRLExecutor expects a SWRLRule input.");
        }

        SWRLRule swrlRule = (SWRLRule) ruleInput;
        SWRLRuleEngine ruleEngine = SWRLAPIFactory.createSWRLRuleEngine(ontology);

        // Save before-inference axioms
        Set<OWLClassAssertionAxiom> beforeClass = ontology.getAxioms(AxiomType.CLASS_ASSERTION);
        Set<OWLObjectPropertyAssertionAxiom> beforeObj = ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
        Set<OWLDataPropertyAssertionAxiom> beforeData = ontology.getAxioms(AxiomType.DATA_PROPERTY_ASSERTION);

        try {
            ruleEngine.createSWRLRule(swrlRule.getRuleName(), swrlRule.getRuleContent());
            // Recursion
            /*String rule0 = "hasParent(?x, ?y) -> hasAncestor(?x, ?y)";
            String rule1 = "hasParent(?x, ?y) ^ hasAncestor(?y, ?z) -> hasAncestor(?x, ?z)";
            ruleEngine.createSWRLRule("baseCase", rule0);
            ruleEngine.createSWRLRule("recursiveCase", rule1);*/
        } catch (Exception e) {
            logger.severe("Invalid SWRL rule: " + e.getMessage());
            SWRLResponseDto swrlResponseDto = new SWRLResponseDto();
            swrlResponseDto.setResults(new ArrayList<>());
            swrlResponseDto.setError(e.getMessage());
            return Collections.singletonList(swrlResponseDto);
        }

        ruleEngine.infer();

        // Get inferred axioms
        Set<OWLClassAssertionAxiom> inferredClass = AxiomUtil.getInferredAxioms(ontology, beforeClass, AxiomType.CLASS_ASSERTION);
        Set<OWLObjectPropertyAssertionAxiom> inferredObj = AxiomUtil.getInferredAxioms(ontology, beforeObj, AxiomType.OBJECT_PROPERTY_ASSERTION);
        Set<OWLDataPropertyAssertionAxiom> inferredData = AxiomUtil.getInferredAxioms(ontology, beforeData, AxiomType.DATA_PROPERTY_ASSERTION);

        // Filter out inferred superclass assertions
        Set<OWLClassAssertionAxiom> filteredClass = filterSuperclassInferences(ontology, inferredClass);

        List<String> results = new ArrayList<>();

        results.addAll(AxiomUtil.formatInferredAxioms(filteredClass, axiom -> {
            String className = axiom.getClassesInSignature().stream().findFirst().map(cls -> cls.getIRI().getShortForm()).orElse("UnknownClass");
            String individualName = axiom.getIndividual().asOWLNamedIndividual().getIRI().getShortForm();
            return className + "(" + individualName + ")";
        }));

        results.addAll(AxiomUtil.formatInferredAxioms(inferredObj, axiom -> {
            String subject = axiom.getSubject().asOWLNamedIndividual().getIRI().getShortForm();
            String object = axiom.getObject().asOWLNamedIndividual().getIRI().getShortForm();
            String property = axiom.getProperty().asOWLObjectProperty().getIRI().getShortForm();
            return property + "(" + subject + ", " + object + ")";
        }));

        results.addAll(AxiomUtil.formatInferredAxioms(inferredData, axiom -> {
            String subject = axiom.getSubject().asOWLNamedIndividual().getIRI().getShortForm();
            String value = axiom.getObject().getLiteral();
            String property = axiom.getProperty().asOWLDataProperty().getIRI().getShortForm();
            return property + "(" + subject + ", " + value + ")";
        }));
        SWRLResponseDto swrlResponseDto = new SWRLResponseDto();
        swrlResponseDto.setResults(results);
        swrlResponseDto.setError("");
        return Collections.singletonList(swrlResponseDto);


    }

    private Set<OWLClassAssertionAxiom> filterSuperclassInferences(OWLOntology ontology, Set<OWLClassAssertionAxiom> inferred) {
        Set<OWLClassAssertionAxiom> filtered = new HashSet<>();

        for (OWLClassAssertionAxiom axiom : inferred) {
            OWLNamedIndividual individual = axiom.getIndividual().asOWLNamedIndividual();
            OWLClass inferredClass = axiom.getClassExpression().asOWLClass();

            // Get all existing classes this individual already belongs to
            Set<OWLClass> assertedClasses = ontology.getClassAssertionAxioms(individual).stream()
                    .map(a -> a.getClassExpression().asOWLClass())
                    .collect(Collectors.toSet());

            boolean isSuperOfAsserted = assertedClasses.stream()
                    .anyMatch(asserted -> isSuperclassOf(ontology, asserted, inferredClass));

            if (!isSuperOfAsserted) {
                filtered.add(axiom);
            }
        }

        return filtered;
    }

    // Checks if superclass is a superclass of subclass in the ontology
    private boolean isSuperclassOf(OWLOntology ontology, OWLClass subclass, OWLClass superclass) {
        return ontology.getSubClassAxiomsForSubClass(subclass).stream()
                .anyMatch(axiom -> axiom.getSuperClass().equals(superclass));
    }
}