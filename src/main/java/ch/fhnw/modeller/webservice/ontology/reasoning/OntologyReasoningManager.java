package ch.fhnw.modeller.webservice.ontology.reasoning;

import ch.fhnw.modeller.webservice.dto.SWRLResponseDto;
import lombok.Getter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.ShortFormProvider;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class OntologyReasoningManager {

    private static OntologyReasoningManager instance;
    public static final Logger logger = Logger.getLogger(OntologyReasoningManager.class.getName());
    private OWLOntologyManager manager;
    @Getter
    private OWLOntology ontology;
    @Getter
    private String ontologyPath;

    public OntologyReasoningManager() {
        this.manager = OWLManager.createOWLOntologyManager();
        logger.info("Ontology Manager built");
    }

    public static OntologyReasoningManager getInstance() {
        if (instance == null) {
            instance = new OntologyReasoningManager();
        }
        return instance;
    }

    public int loadOntology(String ontologyPath) {
        this.ontologyPath = ontologyPath;
        logger.info("Ontology file path: " + ontologyPath);
        try {
            this.ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyPath));
            logger.info("Ontology loaded successfully");
        } catch (OWLOntologyCreationException e) {
            logger.severe(e.getMessage());
            return -1;
        }
        return 1;
    }

    public void loadOntologyFromString(String ttlContent) {
        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            manager.addMissingImportListener(mii ->
                    System.err.println("Missing import (ignored): " + mii.getImportedOntologyURI())
            );

            OWLOntologyLoaderConfiguration cfg = new OWLOntologyLoaderConfiguration()
                    .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT) // don't try to fetch
                    .setFollowRedirects(false); // extra safety

            StringDocumentSource src = new StringDocumentSource(ttlContent, IRI.generateDocumentIRI(), null, null);

            this.ontology = manager.loadOntologyFromOntologyDocument(src, cfg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ontology from TTL string", e);
        }
    }

    public Set<OWLClass> getClasses() {
        return this.ontology.getClassesInSignature();
    }

    public Set<String> getClassNames() {
        return ontology.getClassesInSignature().stream()
                .map(cls -> cls.getIRI().getShortForm())
                .collect(Collectors.toSet());
    }

    public Set<OWLDataProperty> getDataProperties() {
        return this.ontology.getDataPropertiesInSignature();
    }

    public Set<String> getDataPropertyNames() {
        return ontology.getDataPropertiesInSignature().stream()
                .map(dp -> dp.getIRI().getShortForm())
                .collect(Collectors.toSet());
    }

    public Set<OWLObjectProperty> getObjectProperties() {
        return this.ontology.getObjectPropertiesInSignature();
    }

    public Set<String> getObjectPropertyNames() {
        return ontology.getObjectPropertiesInSignature().stream()
                .map(op -> op.getIRI().getShortForm())
                .collect(Collectors.toSet());
    }

    public Set<OWLAnnotationProperty> getAnnotationProperties() {
        return this.ontology.getAnnotationPropertiesInSignature();
    }


    public Set<String> getAnnotationPropertyNames() {
        return ontology.getAnnotationPropertiesInSignature().stream()
                .map(ap -> ap.getIRI().getShortForm())
                .collect(Collectors.toSet());
    }

    public Set<OWLNamedIndividual> getIndividuals() {
        return this.ontology.getIndividualsInSignature();
    }

    public Set<String> getIndividualNames() {
        return ontology.getIndividualsInSignature().stream()
                .map(ind -> ind.getIRI().getShortForm())
                .collect(Collectors.toSet());
    }

    public Set<SWRLResponseDto> getInferredResults(SWRLRule rule) {
        RuleExecutor<SWRLResponseDto> executor = new RuleExecutor<>(new SWRLExecutor());
        return new HashSet<>(executor.execute(this.getOntology(), rule));
    }


    public Map<String, String> getPrefixes() {
        Map<String, String> out = new LinkedHashMap<>();
        OWLDocumentFormat fmt = manager.getOntologyFormat(ontology);

        if (fmt instanceof PrefixDocumentFormat) {
            PrefixDocumentFormat pf = (PrefixDocumentFormat) fmt;
            out.putAll(pf.getPrefixName2PrefixMap());
        }

        // Ensure base ontology IRI has a ":" alias if not declared
        ontology.getOntologyID().getOntologyIRI().asSet().forEach(baseIRI -> {
            out.putIfAbsent(":", baseIRI.toString() + (baseIRI.toString().endsWith("#") ? "" : "#"));
        });

        return out;
    }

    /**
     * Splits "prefix:LocalName" into [prefix, local] or falls back to fragment.
     */
    private static Optional<AbstractMap.SimpleEntry<String, String>> splitPrefixed(OWLEntity e, ShortFormProvider sfp) {
        String sf = sfp.getShortForm(e);
        int idx = sf.indexOf(':');
        if (idx > 0) {
            String p = sf.substring(0, idx);
            String local = sf.substring(idx + 1);
            return Optional.of(new AbstractMap.SimpleEntry<>(p, local));
        }
        // Fallback: IRI fragment
        String frag = e.getIRI().getRemainder().or(() -> {
            String s = e.getIRI().toString();
            int slash = Math.max(s.lastIndexOf('/'), s.lastIndexOf('#'));
            return slash >= 0 ? s.substring(slash + 1) : s;
        });
        return Optional.of(new AbstractMap.SimpleEntry<>("", frag));
    }

    private ShortFormProvider prefixedShortFormProvider() {
        DefaultPrefixManager pm = new DefaultPrefixManager();
        getPrefixes().forEach(pm::setPrefix);
        return new ManchesterOWLSyntaxPrefixNameShortFormProvider(pm);
    }


    private <T extends OWLEntity> Set<String> localNamesByPrefix(Collection<T> entities, Set<String> allowedPrefixes) {
        ShortFormProvider sfp = prefixedShortFormProvider();
        return entities.stream()
                .map(e -> splitPrefixed(e, sfp).orElse(null))
                .filter(Objects::nonNull)
                .filter(pair -> allowedPrefixes.isEmpty() || allowedPrefixes.contains(pair.getKey()))
                .map(AbstractMap.SimpleEntry::getValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> getClassLocalNamesByPrefixes(Collection<String> prefixes) {
        return localNamesByPrefix(
                new ArrayList<>(ontology.getClassesInSignature(Imports.INCLUDED)),
                new HashSet<>(prefixes)
        );
    }

    public Set<String> getObjectPropertyLocalNamesByPrefixes(Collection<String> prefixes) {
        return localNamesByPrefix(
                new ArrayList<>(ontology.getObjectPropertiesInSignature(Imports.INCLUDED)),
                new HashSet<>(prefixes)
        );
    }

    public Set<String> getDataPropertyLocalNamesByPrefixes(Collection<String> prefixes) {
        return localNamesByPrefix(
                new ArrayList<>(ontology.getDataPropertiesInSignature(Imports.INCLUDED)),
                new HashSet<>(prefixes)
        );
    }


}
