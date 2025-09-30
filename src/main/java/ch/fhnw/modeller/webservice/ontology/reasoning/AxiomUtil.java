package ch.fhnw.modeller.webservice.ontology.reasoning;

import org.semanticweb.owlapi.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AxiomUtil {

    /**
     * Returns a set of inferred axioms (of the given type) by comparing the ontology before and after inference.
     *
     * @param ontology     The OWLOntology after inference
     * @param beforeAxioms The set of axioms of the given type before inference
     * @param axiomType    The axiom type to compare
     * @param <T>          The axiom type
     * @return The set of inferred axioms
     */
    public static <T extends OWLAxiom> Set<T> getInferredAxioms(
            OWLOntology ontology,
            Set<T> beforeAxioms,
            AxiomType<T> axiomType
    ) {
        Set<T> afterAxioms = ontology.getAxioms(axiomType);
        Set<T> inferred = new HashSet<>(afterAxioms);
        inferred.removeAll(beforeAxioms);

        return inferred;
    }

    /**
     * Converts axioms to human-readable strings using a formatter.
     */
    public static <T extends OWLAxiom> List<String> formatInferredAxioms(
            Set<T> inferredAxioms,
            Function<T, String> formatter
    ) {
        return inferredAxioms.stream()
                .map(formatter)
                .collect(Collectors.toList());
    }
}