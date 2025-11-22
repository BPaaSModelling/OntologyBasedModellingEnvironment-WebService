package ch.fhnw.modeller.webservice.ontology.reasoning;

import ch.fhnw.modeller.webservice.dto.SWRLResponseDto;
import org.semanticweb.owlapi.model.*;
import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.factory.SWRLAPIFactory;
import org.swrlapi.parser.SWRLParseException;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SWRLExecutor implements RuleExecutionStrategy<SWRLResponseDto> {

    public static final Logger logger = Logger.getLogger(SWRLExecutor.class.getName());

    @Override
    public List<SWRLResponseDto> executeRules(OWLOntology ontology, Object inputRules) {
        List<SWRLRule> splitInputRules = this.extractRuleList(inputRules);
        List<SWRLRule> swrlRules = new ArrayList<>();
        for (SWRLRule inputRule : splitInputRules) {
            if (inputRule == null) {
                throw new IllegalArgumentException("SWRLExecutor expects a SWRLRule input.");
            }
            swrlRules.add(inputRule);
        }


        SWRLRuleEngine ruleEngine = SWRLAPIFactory.createSWRLRuleEngine(ontology);

        // Save before-inference axioms
        Set<OWLClassAssertionAxiom> beforeClass = ontology.getAxioms(AxiomType.CLASS_ASSERTION);
        Set<OWLObjectPropertyAssertionAxiom> beforeObj = ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
        Set<OWLDataPropertyAssertionAxiom> beforeData = ontology.getAxioms(AxiomType.DATA_PROPERTY_ASSERTION);


        for (SWRLRule r : swrlRules) {
            try {
                ruleEngine.createSWRLRule(r.getRuleName(), r.getRuleContent());
            } catch (Exception e) {
                logger.severe("Invalid SWRL rule: " + e.getMessage());
                SWRLResponseDto swrlResponseDto = new SWRLResponseDto();
                swrlResponseDto.setResults(new ArrayList<>());
                swrlResponseDto.setError(e.getMessage());
                return Collections.singletonList(swrlResponseDto);
            }
        }
        // Recursion example
            /*String rule0 = "hasParent(?x, ?y) -> hasAncestor(?x, ?y)";
            String rule1 = "hasParent(?x, ?y) ^ hasAncestor(?y, ?z) -> hasAncestor(?x, ?z)";
            ruleEngine.createSWRLRule("baseCase", rule0);
            ruleEngine.createSWRLRule("recursiveCase", rule1);*/

        ruleEngine.infer();

        ruleEngine.exportInferredOWLAxioms();

        // Get inferred axioms
        Set<OWLClassAssertionAxiom> inferredClass = AxiomUtil.getInferredAxioms(ontology, beforeClass, AxiomType.CLASS_ASSERTION);
        Set<OWLObjectPropertyAssertionAxiom> inferredObj = AxiomUtil.getInferredAxioms(ontology, beforeObj, AxiomType.OBJECT_PROPERTY_ASSERTION);
        Set<OWLDataPropertyAssertionAxiom> inferredData = AxiomUtil.getInferredAxioms(ontology, beforeData, AxiomType.DATA_PROPERTY_ASSERTION);

        // Filter out inferred superclass assertions
        Set<OWLClassAssertionAxiom> filteredClass = filterSuperclassInferences(ontology, inferredClass);

        List<String> results = new ArrayList<>();

        results.addAll(AxiomUtil.formatInferredAxioms(filteredClass, axiom ->

        {
            String className = axiom.getClassesInSignature().stream().findFirst().map(cls -> cls.getIRI().getShortForm()).orElse("UnknownClass");
            String individualName = axiom.getIndividual().asOWLNamedIndividual().getIRI().getShortForm();
            return className + "(" + individualName + ")";
        }));

        results.addAll(AxiomUtil.formatInferredAxioms(inferredObj, axiom ->

        {
            String subject = axiom.getSubject().asOWLNamedIndividual().getIRI().getShortForm();
            String object = axiom.getObject().asOWLNamedIndividual().getIRI().getShortForm();
            String property = axiom.getProperty().asOWLObjectProperty().getIRI().getShortForm();
            return property + "(" + subject + ", " + object + ")";
        }));

        results.addAll(AxiomUtil.formatInferredAxioms(inferredData, axiom ->

        {
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

    private List<SWRLRule> extractRuleList(Object inputRules) {
        if (inputRules == null) {
            return Collections.emptyList();
        }
        SWRLRule swrlRule = (SWRLRule) inputRules;
        String ruleString = swrlRule.getRuleContent();

        if (ruleString.isEmpty()) {
            return Collections.emptyList();
        }

        // CASE 1: no ";" -> treat the whole string as a single rule
        if (!ruleString.contains(";")) {
            return Collections.singletonList(swrlRule);
        }

        // CASE 2: one or more ";" -> split into multiple rules
        List<String> splitRules = Arrays.stream(ruleString.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        List<SWRLRule> rules = new ArrayList<>();
        int i = 0;
        for (String s : splitRules) {
            SWRLRule splitWwrlRules = new SWRLRule(swrlRule.getRuleName() + "_" + i, s);
            rules.add(splitWwrlRules);
            i++;
        }
        return rules;
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