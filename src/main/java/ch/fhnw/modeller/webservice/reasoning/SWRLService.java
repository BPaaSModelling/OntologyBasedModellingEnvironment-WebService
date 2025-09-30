package ch.fhnw.modeller.webservice.reasoning;

import ch.fhnw.modeller.webservice.dto.SWRLDto;
import ch.fhnw.modeller.webservice.dto.SWRLResponseDto;
import ch.fhnw.modeller.webservice.ontology.reasoning.OntologyReasoningManager;
import com.google.gson.Gson;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Path("/OntologyAndSWRL")
public class SWRLService {

    private final Logger logger = Logger.getLogger(SWRLService.class.getName());
    private static final Gson gson = new Gson();


    @POST
    @Path("/glossary")
    @Consumes("text/turtle")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGlossary(String ttlContent) {
        OntologyReasoningManager manager = OntologyReasoningManager.getInstance();
        manager.loadOntologyFromString(ttlContent);
        List<String> classNames = manager.getClasses().stream().map(Object::toString).collect(Collectors.toList());
        String payload = gson.toJson(classNames);
        logger.info("PAYLOAD: " + payload);
        return Response.ok(payload).build();
    }

    @POST
    @Path("/relations")
    @Consumes("text/turtle")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRelations(String ttlContent) {
        OntologyReasoningManager manager = OntologyReasoningManager.getInstance();
        manager.loadOntologyFromString(ttlContent);
        List<String> relationNames = new ArrayList<>(manager.getObjectPropertyNames());
        String payload = gson.toJson(relationNames);
        logger.info("PAYLOAD: " + payload);
        return Response.ok(payload).build();
    }

    @POST
    @Path("/applySWRLRule")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response applySWRLRule(SWRLDto dto) {
        OntologyReasoningManager manager = OntologyReasoningManager.getInstance();
        manager.loadOntologyFromString(dto.getTtlContent());

        List<SWRLResponseDto> inferredResults = new ArrayList<>(manager.getInferredResults(dto.getRule()));
        logger.info("Inferred Results: " + inferredResults);
        String payload = gson.toJson(inferredResults);
        return Response.ok(payload).build();
    }

}


