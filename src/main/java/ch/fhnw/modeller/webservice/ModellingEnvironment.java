package ch.fhnw.modeller.webservice;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.swing.*;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import ch.fhnw.modeller.auth.UserService;
import ch.fhnw.modeller.model.metamodel.*;
import ch.fhnw.modeller.model.model.Model;
import ch.fhnw.modeller.model.model.ModellingLanguageConstructInstance;
import ch.fhnw.modeller.webservice.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.MethodNotSupportedException;
import org.apache.jena.graph.Node;
import org.apache.jena.query.*;

import com.google.gson.Gson;

import ch.fhnw.modeller.model.graphEnvironment.Answer;
import ch.fhnw.modeller.model.palette.PaletteCategory;
import ch.fhnw.modeller.model.palette.PaletteElement;
import ch.fhnw.modeller.webservice.exception.NoResultsException;
import ch.fhnw.modeller.webservice.ontology.FormatConverter;
import ch.fhnw.modeller.webservice.ontology.NAMESPACE;
import ch.fhnw.modeller.webservice.ontology.OntologyManager;
import ch.fhnw.modeller.persistence.GlobalVariables;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.engine.Shacl;
import org.apache.jena.shacl.engine.ShaclPaths;
import org.apache.jena.shacl.lib.ShLib;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.shacl.validation.Severity;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.vocabulary.*;
import org.eclipse.rdf4j.model.Triple;
import org.json.JSONArray;
import org.json.JSONObject;

import static ch.fhnw.modeller.webservice.ontology.NAMESPACE.MODEL;
import static ch.fhnw.modeller.webservice.ontology.NAMESPACE.SH;

@Path("/ModEnv")
public class ModellingEnvironment {
	private Gson gson = new Gson();
	private OntologyManager ontology = OntologyManager.getInstance();
	private boolean debug_properties = false;

	//Allows to retrieve the userService
	@Context
	private ContainerRequestContext crc;
	private String sListTtlfromGithub = "https://api.github.com/repos/BPaaSModelling/Ontology4ModelingEnvironment/contents/";
	//private String sRawContentTtlFromGithub= "https://raw.githubusercontent.com/BPaaSModelling/Ontologies4Import/main/";
	private String sRawContentTtlFromGithub = "https://raw.githubusercontent.com/BPaaSModelling/Ontology4ModelingEnvironment/master/";


	private String extractIdFrom(QuerySolution querySolution, String label) {

		if (querySolution.get(label) == null) return null;

		String value = querySolution.get(label).toString();
		return value.contains("#") ? value.split("#")[1] : value;
	}

	private String extractNamespaceAndIdFrom(QuerySolution querySolution, String label) {

		if (querySolution.get(label) == null) return null;

		String value = querySolution.get(label).toString();
		return value.contains("#") ? GlobalVariables.getNamespaceMap().get(value.split("#")[0]) + ":" + value.split("#")[1] : value;
	}

	private String extractValueFrom(QuerySolution querySolution, String label) {
		return querySolution.get(label) != null ? querySolution.get(label).toString() : null;
	}


	@GET
	@Path("/model")
	public Response getAllModels() {
		String command = String.format(
				"SELECT ?model ?label " +
						"WHERE { " +
						"?model rdf:type %s:Model . " +
						"?model rdfs:label ?label " +
						"}",
				MODEL.getPrefix());

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		
		ResultSet resultSet = ontology.query(query).execSelect();

		ArrayList<Model> models = new ArrayList<>();

		while (resultSet.hasNext()) {
			QuerySolution next = resultSet.next();
			String id = extractIdFrom(next, "?model");
			String label = extractValueFrom(next, "?label");
			models.add(new Model(id, label));
		}

		String payload = gson.toJson(models);

		return Response.status(Status.OK).entity(payload).build();
	}

	@DELETE
	@Path("/model/{modelId}")
	public Response deleteModel(@PathParam("modelId") String modelId) {

		for (ModelElementDetailDto modelElementDetailDto : this.getModelElementDetailDtos(modelId)) {
			this.deleteElementOfModel(modelId, modelElementDetailDto.getId());
		}

		ParameterizedSparqlString deleteQuery = getDeleteModelQuery(modelId);
		
		ontology.insertQuery(deleteQuery);

		return Response.status(Status.OK).build();
	}

	@PUT
	@Path("/model/{modelId}")
	public Response updateModel(@PathParam("modelId") String modelId, String json) {

		ModelUpdateDto modelUpdateDto = gson.fromJson(json, ModelUpdateDto.class);

		String deleteModelLabel = String.format(
				"DELETE {\n" +
						"\t%1$s:%2$s rdfs:label ?label\n" +
						"}\n" +
						"WHERE\n" +
						"{\n" +
						"\t%1$s:%2$s rdfs:label ?label\n" +
						"}",
				MODEL.getPrefix(),
				modelId);

		String insertModelLabel = String.format(
				"INSERT DATA {\n" +
						"\t%1$s:%2$s rdfs:label \"%3$s\"\n" +
						"}",
				MODEL.getPrefix(),
				modelId,
				modelUpdateDto.getLabel());

		ParameterizedSparqlString deleteQuery = new ParameterizedSparqlString(deleteModelLabel);
		ParameterizedSparqlString insertQuery = new ParameterizedSparqlString(insertModelLabel);

		
		ontology.insertQuery(deleteQuery);
		ontology.insertQuery(insertQuery);

		return Response.status(Status.OK).build();
	}

	private ParameterizedSparqlString getDeleteModelQuery(String modelId) {

		String command = String.format(
				"DELETE {\n" +
						"\t%1$s:%2$s ?modelRel ?modelObj .\n" +
						"\t?diag ?diagRel ?diagObj .\n" +
						"\t?referencingDiag %1$s:shapeRepresentsModel %1$s:%2$s\n" +
						"}\n" +
						"WHERE {\n" +
						"\t%1$s:%2$s ?modelRel ?modelObj .\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:modelHasShape ?diag .\n" +
						"\t?diag ?diagRel ?diagObj } .\n" +
						"\tOPTIONAL { ?referencingDiag %1$s:shapeRepresentsModel %1$s:%2$s}\n" +
						"}",
				MODEL.getPrefix(),
				modelId
		);

		return new ParameterizedSparqlString(command);
	}

	@POST
	@Path("/model")
	public Response createModel(String json) {

		Gson gson = new Gson();
		ModelCreationDto modelCreationDto = gson.fromJson(json, ModelCreationDto.class);

		String modelId = String.format("Model_%s", UUID.randomUUID().toString());

		String command = String.format(
				"INSERT DATA { " +
						"%1$s:%2$s rdf:type %1$s:Model ." +
						"%1$s:%2$s rdfs:label \"%3$s\" " +
						"}",
				MODEL.getPrefix(),
				modelId,
				modelCreationDto.getLabel());

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		
		ontology.insertQuery(query);

		String selectCommand = String.format(
				"SELECT ?label " +
						"WHERE { " +
						"%1$s:%2$s rdf:type %1$s:Model . " +
						"%1$s:%2$s rdfs:label ?label " +
						"}",
				MODEL.getPrefix(),
				modelId);

		ParameterizedSparqlString selectQuery = new ParameterizedSparqlString(selectCommand);
		ResultSet resultSet = ontology.query(selectQuery).execSelect();

		if (!resultSet.hasNext()) {
			throw new IllegalStateException("created model can not be queried");
		}

		QuerySolution next = resultSet.next();
		String label = extractValueFrom(next, "?label");
		Model createdModel = new Model(modelId, label);

		String payload = gson.toJson(createdModel);

		return Response.status(Status.CREATED).entity(payload).build();
	}
	private static final Map<String, CompletableFuture<List<ModelElementDetailDto>>> taskMap = new ConcurrentHashMap<>();
	@GET
	@Path("/model/{id}/element")
	public Response getModelElementList(@PathParam("id") String id) {
		// Start the task asynchronously
		CompletableFuture<List<ModelElementDetailDto>> futureTask = taskMap.get(id);
		if (futureTask == null || futureTask.isDone()) {
			futureTask = CompletableFuture.supplyAsync(() -> getModelElementDetailDtos(id));
			taskMap.put(id, futureTask);
		}
		System.out.println("getModelElementList started for model " + id);
		// Immediately return a response indicating the task is in progress
		return Response.status(Status.ACCEPTED).entity(gson.toJson(id)).build();
	}

	@GET
	@Path("/model/{id}/element/status")
	public Response getModelElementStatus(@PathParam("id") String id) {
		CompletableFuture<List<ModelElementDetailDto>> futureTask = taskMap.get(id);
		if (futureTask == null) {
			Logger logger =  Logger.getLogger(ModellingEnvironment.class.getName());
			logger.warning("getModelElementStatus: Task not found for model " + id);
			return Response.status(Status.NOT_FOUND).build();
		} else if (!futureTask.isDone()) {
			return Response.status(Status.ACCEPTED).entity(gson.toJson("Processing elements in progress")).build();
		} else {
			try {
				List<ModelElementDetailDto> elements = futureTask.get();

				System.out.println("getModelElementList completed for model " + id);
				return Response.status(Status.OK).entity(gson.toJson(elements)).build();
			} catch (InterruptedException | ExecutionException e) {
				System.out.println("getModelElementList Exception for model " + id + ": " + e.getMessage());
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(gson.toJson("Error processing task")).build();
			} finally {
				//taskMap.remove(id);
 			}
		}
	}

	private List<ModelElementDetailDto> getModelElementDetailDtos(String id) {
		String modelId = String.format("%s:%s", MODEL.getPrefix(), id);

		String command = String.format(
				"SELECT ?diag\n" +
						"WHERE {\n" +
						"\t%1$s %2$s:modelHasShape ?diag .\n" +
						"}",
				modelId,
				MODEL.getPrefix()
		);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		List<String> shapeIds = new ArrayList<>();

		while (resultSet.hasNext()) {
			QuerySolution solution = resultSet.next();
			shapeIds.add(extractIdFrom(solution, "?diag"));
		}

		List<ModelElementDetailDto> modelElements = new ArrayList<>(shapeIds.size());

		shapeIds.forEach(shapeId -> {
			Map<String, String> shapeAttributes = getShapeAttributes(shapeId);
			PaletteVisualInformationDto visualInformationDto = getPaletteVisualInformation(shapeId);

			String modelElementId = shapeAttributes.get("shapeVisualisesConceptualElement").split("#")[1];
			AbstractElementAttributes abstractElementAttributes = getModelElementAttributesAndOptions(modelElementId);

			abstractElementAttributes.getModelElementType()
					.ifPresent(elementType -> modelElements.add(
							ModelElementDetailDto.from(
									shapeId,
									shapeAttributes,
									abstractElementAttributes,
									elementType,
									visualInformationDto)));
		});
		return modelElements;
	}

	private PaletteVisualInformationDto getPaletteVisualInformation(String shapeId) {

		String command = String.format(
				"SELECT ?imgUrl ?cat ?fromArrow ?toArrow ?arrowStroke\n" +
						"WHERE\n" +
						"{\n" +
						"\t%1$s:%2$s %1$s:shapeInstantiatesPaletteConstruct ?po .\n" +
						"\t?po po:paletteConstructIsGroupedInPaletteCategory ?cat .\n" +
						"\tOPTIONAL { ?po po:paletteConstructHasModelImage ?imgUrl .}\n" +
						"\tOPTIONAL { ?po po:paletteConnectorConfiguresFromArrowHead ?fromArrow .}\n" +
						"\tOPTIONAL { ?po po:paletteConnectorConfiguresToArrowHead ?toArrow .}\n" +
						"\tOPTIONAL { ?po po:paletteConnectorConfiguresArrowStroke ?arrowStroke .}\n" +
						"}",
				MODEL.getPrefix(),
				shapeId);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		QuerySolution querySolution = resultSet.next();
		String category = extractIdFrom(querySolution, "?cat");
		String imageName = extractValueFrom(querySolution, "?imgUrl");
		String imageUrl = imageName != null ? category + "/" + imageName : null;

		String fromArrow = extractIdFrom(querySolution, "?fromArrow");
		String toArrow = extractIdFrom(querySolution, "?toArrow");
		String arrowStroke = extractIdFrom(querySolution, "?arrowStroke");

		PaletteVisualInformationDto dto = new PaletteVisualInformationDto();
		dto.setFromArrow(fromArrow);
		dto.setToArrow(toArrow);
		dto.setImageUrl(imageUrl);
		dto.setArrowStroke(arrowStroke);

		return dto;
	}

	private ModelElementDetailDto getModelElementDetail(String modelId, String shapeId) {
		String modelIdentifier = String.format("%s:%s", MODEL.getPrefix(), modelId);

		Map<String, String> shapeAttributes = getShapeAttributes(shapeId);
		String modelElement = shapeAttributes.get("shapeVisualisesConceptualElement");
		String modelElementId = modelElement.split("#")[1];
		AbstractElementAttributes abstractElementAttributes = getModelElementAttributesAndOptions(modelElementId);

		PaletteVisualInformationDto visualInformationDto = getPaletteVisualInformation(shapeId);

		return abstractElementAttributes.getModelElementType()
				.map(elementType -> ModelElementDetailDto.from(shapeId, shapeAttributes, abstractElementAttributes, elementType, visualInformationDto))
				.orElse(null);
	}

	private Optional<ModelElementType> getModelElementType(String modelElementId) {

		String command = String.format(
				"SELECT ?type ?class ?allTypes \n" +
						"WHERE { \n" +
						"\t{\n" +
						"\t%1$s:%2$s rdf:type %1$s:ConceptualElement . \n" +
						"\t%1$s:%2$s rdf:type ?allTypes . \n" +
						"\t%1$s:%2$s rdfs:subClassOf ?class . \n" +
						"\t%1$s:%2$s rdfs:subClassOf* ?type . \n" +
						"\t?type rdfs:subClassOf lo:ModelingLanguageConstruct \n" +
						"\t}\n" +
						"\tUNION\n" +
						"\t{\n" +
						"\t%1$s:%2$s rdf:type %1$s:ConceptualElement . \n" +
						"\t%1$s:%2$s rdf:type ?allTypes . \n" +
						"\t%1$s:%2$s rdf:type ?class .\n" +
						"\t?class rdfs:subClassOf* ?type . \n" +
						"\t?type rdfs:subClassOf lo:ModelingLanguageConstruct \n" +
						"\t}\n" +
						"}",
				MODEL.getPrefix(),
				modelElementId
		);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		System.out.println("Executing query: " + query.toString());
		ResultSet resultSet = ontology.query(query).execSelect();

		List<String> types = new ArrayList<>();

		ModelElementType modelElementType = new ModelElementType();

		if (resultSet.hasNext()) {
			QuerySolution next = resultSet.next();
			String type = extractIdFrom(next, "?type");
			String directSuperClass = extractIdFrom(next, "?class");
			String firstType = extractNamespaceAndIdFrom(next, "?allTypes");

			types.add(firstType);

			if (type != null) {
				modelElementType.setType(type);
				modelElementType.setModellingLanguageConstruct(directSuperClass);
			}
		}

		if ("ModelingElement".equals(modelElementType.getType())) {
			while (resultSet.hasNext()) {
				QuerySolution next = resultSet.next();
				String type = extractIdFrom(next, "?type");
				if ("ModelingContainer".equals(type)) {
					modelElementType.setType(type);
				}
			}
		}

		if (resultSet.hasNext()) {
			QuerySolution nextForOtherType = resultSet.next();
			String otherType = extractNamespaceAndIdFrom(nextForOtherType, "?allTypes");
			types.add(otherType);
		}

		if (modelElementType.getType() != null) {
			if (types.contains("owl:Class")) {
				modelElementType.setInstantiationType(InstantiationTargetType.Class);
			} else {
				modelElementType.setInstantiationType(InstantiationTargetType.Instance);

			}

			return Optional.of(modelElementType);
		}

		return Optional.empty();
	}

	private Map<String, String> getShapeAttributes(String id) {

		Map<String, String> attributes = new HashMap<>();

		String command = String.format(
				"SELECT *\n" +
						"WHERE {\n" +
						"\t%1$s:%2$s ?rel ?relValue .\n" +
						"\t%1$s:%2$s rdf:type %1$s:Shape\n" +
						"}",
				MODEL.getPrefix(),
				id
		);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		while (resultSet.hasNext()) {
			QuerySolution solution = resultSet.next();
			String relation = extractIdFrom(solution, "?rel");
			String value = extractValueFrom(solution, "?relValue");

			attributes.putIfAbsent(relation, value);
		}
		return attributes;
	}

	private Map<String, String> getModelElementAttributes(String modelElementId) {

		Map<String, String> attributes = new HashMap<>();

		String command = String.format(
				"SELECT *\n" +
						"WHERE {\n" +
						"\t%1$s:%2$s rdf:type %1$s:ConceptualElement .\n" +
						"\t%1$s:%2$s ?rel ?relValue .\n" +
						"}",
				MODEL.getPrefix(),
				modelElementId
		);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		while (resultSet.hasNext()) {
			QuerySolution solution = resultSet.next();
			String relation = extractValueFrom(solution, "?rel");
			String value = extractValueFrom(solution, "?relValue");

			attributes.putIfAbsent(relation, value);
		}
		return attributes;
	}

	private AbstractElementAttributes getModelElementAttributesAndOptions(String modelElementId) {

		Optional<ModelElementType> elementTypeOpt = getModelElementType(modelElementId);

		if (!elementTypeOpt.isPresent()) return null;

		String instanceCreationBit = "\t\t%1$s:%2$s rdf:type ?mloConcept .\n";
		String classCreationBit = "\t\t%1$s:%2$s rdf:type owl:Class .\n" +
				"\t\t%1$s:%2$s rdfs:subClassOf ?mloConcept .\n";

		String creationBit = elementTypeOpt.get().getInstantiationType() == InstantiationTargetType.Class ? classCreationBit : instanceCreationBit;

		String command = String.format(
				"SELECT ?mloConcept ?objProp ?range ?instanceValue ?classValue\n" +
						"WHERE {\n" +
						creationBit +
						"\t\t?mloConcept rdfs:subClassOf* ?relTarget .\n" +
						"\n" +
						"\t\t?objProp rdf:type ?objPropType .\n" +
						"\t\tFILTER(?objPropType IN (owl:ObjectProperty, owl:DatatypeProperty)) .\n" +
						"\t\t?objProp rdfs:range ?range\n" +
						"\n" +
						"\t\tFILTER EXISTS {\n" +
						"\t\t\t?objProp rdfs:subPropertyOf* ?superObjProp .\n" +
						"\t\t\t?superObjProp rdfs:domain ?relTarget .\n" +
						"\n" +
						"\t\t\tFILTER NOT EXISTS {\n" +
						"\t\t\t\t?subObjProp rdfs:subPropertyOf+ ?superObjProp .\n" +
						"\t\t\t\t?subObjProp rdfs:domain ?overwritingDomain .\n" +
						"\t\t\t\t?objProp rdfs:subPropertyOf* ?subObjProp\n" +
						"\t\t\t}\n" +
						"\t\t}\n" +
						"\n" +
						"\t\tFILTER EXISTS {\n" +
						"\t\t\t?objProp rdfs:subPropertyOf* ?superObjProp .\n" +
						"\t\t\t?superObjProp %1$s:propertyIsShownInModel true .\n" +
						"\n" +
						"\t\t\tFILTER NOT EXISTS {\n" +
						"\t\t\t\t?subObjProp rdfs:subPropertyOf+ ?superObjProp .\n" +
						"\t\t\t\t?subObjProp %1$s:propertyIsShownInModel false .\n" +
						"\t\t\t\t?objProp rdfs:subPropertyOf* ?subObjProp\n" +
						"\t\t\t}\n" +
						"\t\t}\n" +
						"\tOPTIONAL\n" +
						"\t{\n" +
						"\t\t%1$s:%2$s ?objProp ?instanceValue\n" +
						"\t}\n" +
						"\n" +
						"\tOPTIONAL\n" +
						"\t{\n" +
						"\t\t%1$s:%2$s rdf:type ?mloTypeForValue .\n" +
						"\t\t?mloTypeForValue rdfs:subClassOf* ?relTargetForValue .\n" +
						"\t\t?relTargetForValue ?objProp ?classValue\n" +
						"\t}\n" +
						"}",
				MODEL.getPrefix(),
				modelElementId
		);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		Set<RelationDto> options = new HashSet<>();
		List<ModelElementAttribute> values = new ArrayList<>();

		while (resultSet.hasNext()) {
			QuerySolution solution = resultSet.next();
			String relation = extractValueFrom(solution, "?objProp");
			String instanceValue = extractValueFrom(solution, "?instanceValue");
			String classValue = extractValueFrom(solution, "?classValue");
			String actualValue = instanceValue != null ? instanceValue : classValue;

			RelationDto relationDto = new RelationDto(GlobalVariables.getNamespaceMap().get(relation.split("#")[0]), relation.split("#")[1]);
			options.add(relationDto);

			if (actualValue != null) {
				boolean valueHasNamespace = false;

				if (actualValue.contains("#")) {
					String potentialNamespace = actualValue.split("#")[0];
					valueHasNamespace = GlobalVariables.getPrefixMap().containsValue(potentialNamespace.concat("#"));
				}

				if (valueHasNamespace) {
					ModelElementAttribute value = new ModelElementAttribute();
					value.setRelation(relationDto.getRelation());
					value.setRelationPrefix(relationDto.getRelationPrefix());
					value.setValue(GlobalVariables.getNamespaceMap().get(actualValue.split("#")[0]) + ":" + actualValue.split("#")[1]);

					values.add(value);
				} else {
					ModelElementAttribute value = new ModelElementAttribute();
					value.setRelation(relationDto.getRelation());
					value.setRelationPrefix(relationDto.getRelationPrefix());

					if (!actualValue.contains("#")) { // string typings are omitted and must be added manually
						actualValue = actualValue.concat("^^" + NAMESPACE.XSD.getURI() + "string");
					}

					String key = actualValue.split("#")[0];
					String primitiveValuePart = key.split("\\^\\^")[0];
					String namespacePart = key.split("\\^\\^")[1];
					String actualNamespacePart = GlobalVariables.getNamespaceMap().get(namespacePart);
					String typePart = actualValue.split("#")[1];
					value.setValue(String.format("\"%s\"^^%s:%s", primitiveValuePart, actualNamespacePart, typePart));

					values.add(value);
				}
			}

		}

		List<String> referencingShapes = getReferencingShapeIds(modelElementId);

		return new AbstractElementAttributes(
				elementTypeOpt.get().getModellingLanguageConstruct(),
				options,
				values,
				elementTypeOpt.get().getType(),
				elementTypeOpt.get().getInstantiationType(),
				referencingShapes);
	}

	private List<String> getReferencingShapeIds(String modelElementId) {
		String incomingReferencesCommand = String.format(
				"SELECT ?diag\n" +
						"WHERE { \n" +
						"\t?diag %1$s:shapeVisualisesConceptualElement %1$s:%2$s .\n" +
						"}",
				MODEL.getPrefix(),
				modelElementId);

		List<String> referencingShapes = new ArrayList<>();

		ParameterizedSparqlString incomingReferencesQuery = new ParameterizedSparqlString(incomingReferencesCommand);
		ResultSet incomingReferencesResultSet = ontology.query(incomingReferencesQuery).execSelect();
		while (incomingReferencesResultSet.hasNext()) {
			QuerySolution next = incomingReferencesResultSet.next();
			String shape = extractIdFrom(next, "?diag");
			referencingShapes.add(shape);
		}
		return referencingShapes;
	}

	@PUT
	@Path("/model/{modelId}/element")
	public Response createModelElement(@PathParam("modelId") String modelId,
									   String json) {

		Gson gson = new Gson();
		ModelElementCreationDto modelElementCreationDto = gson.fromJson(json, ModelElementCreationDto.class);

		if (modelElementCreationDto.getModelingLanguageConstructInstance() == null) {
			createShapeForNewModelElement(modelId, modelElementCreationDto);
		} else {
			ParameterizedSparqlString query = getShapeCreationQuery(modelElementCreationDto, modelId, modelElementCreationDto.getModelingLanguageConstructInstance());
			ontology.insertQuery(query);
		}

		ModelElementDetailDto modelElement = getModelElementDetail(modelId, modelElementCreationDto.getUuid());
		return Response.status(Status.CREATED).entity(gson.toJson(modelElement)).build();
	}

	private ParameterizedSparqlString getShapeAndAbstractElementCreationQuery(ModelElementCreationDto modelElementCreationDto, String modelId, String elementId) {
		String instanceCreationBit = "	%7$s:%1$s rdf:type ?type .\n";
		String classCreationBit = "	%7$s:%1$s rdf:type owl:Class .\n" +
				"	%7$s:%1$s rdfs:subClassOf ?type .\n";

		String creationBit = modelElementCreationDto.getInstantiationType() == InstantiationTargetType.Class ? classCreationBit : instanceCreationBit;

		String command = String.format(
				"INSERT {\n" +
						creationBit +
						"	%7$s:%1$s rdf:type %7$s:ConceptualElement .\n" +
						"	%7$s:%1$s lo:elementIsMappedWithDOConcept ?concept .\n" +
						"	%7$s:%2$s rdf:type %7$s:Shape .\n" +
						"	%7$s:%2$s %7$s:shapePositionsOnCoordinateX %5$s .\n" +
						"	%7$s:%2$s %7$s:shapePositionsOnCoordinateY %6$s .\n" +
						"	%7$s:%2$s %7$s:shapeHasHeight ?height .\n" +
						"	%7$s:%2$s %7$s:shapeHasWidth ?width .\n" +
						"	%7$s:%2$s %7$s:shapeInstantiatesPaletteConstruct po:%3$s .\n" +
						"	%7$s:%2$s %7$s:shapeVisualisesConceptualElement %7$s:%1$s .\n" +
						"	%7$s:%2$s rdfs:label \"%8$s\" .\n" +
						"	%7$s:%4$s %7$s:modelHasShape %7$s:%2$s .\n" +
						"}" +
						"WHERE {" +
						"	po:%3$s po:paletteConstructIsRelatedToModelingLanguageConstruct ?type .\n" +
						"	po:%3$s po:paletteConstructHasHeight ?height .\n" +
						"	po:%3$s po:paletteConstructHasWidth ?width .\n" +
						"	OPTIONAL { ?type lo:elementIsMappedWithDOConcept ?concept }\n" +
						"}",
				elementId,
				modelElementCreationDto.getUuid(),
				modelElementCreationDto.getPaletteConstruct(),
				modelId,
				modelElementCreationDto.getX(),
				modelElementCreationDto.getY(),
				MODEL.getPrefix(),
				modelElementCreationDto.getLabel()
		);

		return new ParameterizedSparqlString(command);
	}

	private void createShapeForNewModelElement(String modelId, ModelElementCreationDto modelElementCreationDto) {
		Optional<String> mappedModelingLanguageConstruct = getMappedModelingLanguageConstruct(modelElementCreationDto.getPaletteConstruct());

		if (!mappedModelingLanguageConstruct.isPresent()) {
			throw new IllegalArgumentException("Palette Construct must be related to a Modeling Language Construct");
		}

		String elementId = String.format("%s_%s", mappedModelingLanguageConstruct.get(), UUID.randomUUID().toString());

		ParameterizedSparqlString query = getShapeAndAbstractElementCreationQuery(modelElementCreationDto, modelId, elementId);
		ontology.insertQuery(query);
	}

	@PUT
	@Path("/model/{modelId}/connection")
	public Response createConnection(@PathParam("modelId") String modelId,
									 String json) {

		Gson gson = new Gson();
		ConnectionCreationDto connectionCreationDto = gson.fromJson(json, ConnectionCreationDto.class);

		Optional<String> mappedModelingLanguageConstruct = getMappedModelingLanguageConstruct(connectionCreationDto.getPaletteConstruct());

		if (!mappedModelingLanguageConstruct.isPresent()) {
			throw new IllegalArgumentException("Palette Construct must be related to a Modeling Language Construct");
		}

		String shapeId = String.format("%s_Shape_%s",
				connectionCreationDto.getPaletteConstruct(),
				connectionCreationDto.getUuid());

		String elementId = String.format("%s_%s", mappedModelingLanguageConstruct.get(), UUID.randomUUID().toString());

		String command = getConnectionCreationCommand(connectionCreationDto, modelId, shapeId, elementId);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ontology.insertQuery(query);

		ModelElementDetailDto modelElement = getModelElementDetail(modelId, shapeId);
		return Response.status(Status.CREATED).entity(gson.toJson(modelElement)).build();
	}

	@DELETE
	@Path("/model/{modelId}/element/{shapeId}")
	public Response deleteModelElement(@PathParam("modelId") String modelId,
									   @PathParam("shapeId") String shapeId) {

		deleteElementOfModel(modelId, shapeId);

		return Response.status(Status.OK).build();
	}

	private void deleteElementOfModel(String modelId, String shapeId) {
		ModelElementDetailDto modelElementDetailDto = getModelElementDetail(modelId, shapeId);
		String conceptualElementId = modelElementDetailDto.getModelingLanguageConstructInstance();

		ParameterizedSparqlString deleteQuery = getDeleteShapeQuery(shapeId);
		ontology.insertQuery(deleteQuery);

		if (StringUtils.isNotBlank(conceptualElementId)
				&& !isModelingLanguageConstructLinkedInAnotherModel(conceptualElementId)) {
			deleteModelElementInstance(conceptualElementId);
		}
	}

	private boolean isModelingLanguageConstructLinkedInAnotherModel(String instanceId) {

		String command = String.format(
				"SELECT *\n" +
						"WHERE {\n" +
						"    ?shape %1$s:shapeVisualisesConceptualElement %1$s:%2$s\n" +
						"}",
				MODEL.getPrefix(),
				instanceId
		);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		return resultSet.hasNext();
	}

	@PUT
	@Path("/model/{modelId}/element/{shapeId}")
	public Response updateModelElement(@PathParam("modelId") String modelId,
									   @PathParam("shapeId") String shapeId,
									   String json) {

		ModelElementDetailDto modelElementDetailDto = getModelElementDetail(modelId, shapeId);

		if (modelElementDetailDto == null) {
			return Response.status(Status.NOT_FOUND).build();
		}

		Gson gson = new Gson();
		ModelElementDetailDto modelElementToBeStored = gson.fromJson(json, ModelElementDetailDto.class);

		List<ParameterizedSparqlString> queries = new ArrayList<>();

		ParameterizedSparqlString deleteQuery = getDeleteShapeDataQuery(shapeId);
		queries.add(deleteQuery);

		ParameterizedSparqlString insertQuery = getInsertShapeDataQuery(shapeId, modelElementToBeStored);
		queries.add(insertQuery);

		if (modelElementToBeStored.hasOptionalValues()) {
			ParameterizedSparqlString optInsertQuery = getInsertOptionalShapeDataQuery(shapeId, modelElementToBeStored);
			queries.add(optInsertQuery);
		}

		if (modelElementDetailDto.getAbstractElementAttributes() != null &&
				modelElementDetailDto.getAbstractElementAttributes().getValues() != null &&
				!modelElementDetailDto.getAbstractElementAttributes().getValues().isEmpty()) {

			ParameterizedSparqlString deleteQueryElement = getDeleteModelElementDataQuery(modelElementDetailDto.getModelingLanguageConstructInstance(), modelElementDetailDto.getAbstractElementAttributes().getValues());
			queries.add(deleteQueryElement);
		}

		if (modelElementDetailDto.getModelingLanguageConstructInstance().equals(modelElementToBeStored.getModelingLanguageConstructInstance()) &&
				modelElementToBeStored.getAbstractElementAttributes() != null &&
				modelElementToBeStored.getAbstractElementAttributes().getValues() != null &&
				!modelElementToBeStored.getAbstractElementAttributes().getValues().isEmpty()) {

			Optional<ParameterizedSparqlString> insertModelElementDataQuery = getInsertModelElementDataQuery(modelElementDetailDto.getModelingLanguageConstructInstance(), modelElementToBeStored.getAbstractElementAttributes().getValues());
			insertModelElementDataQuery.ifPresent(queries::add);
		}

		if ("ModelingContainer".equals(modelElementDetailDto.getModelElementType())) {
			ParameterizedSparqlString deleteQueryElement = getDeleteContainedModelElementsQuery(modelElementDetailDto.getModelingLanguageConstructInstance());
			queries.add(deleteQueryElement);

			if (modelElementToBeStored.getContainedShapes() != null && !modelElementToBeStored.getContainedShapes().isEmpty()) {
				ParameterizedSparqlString insertQueryElement = getInsertContainedModelElementsQuery(modelElementToBeStored.getModelingLanguageConstructInstance(), modelElementToBeStored.getContainedShapes());
				queries.add(insertQueryElement);
			}
		}
		ontology.insertMultipleQueries(queries);

		ModelElementDetailDto modelElementDetail = getModelElementDetail(modelId, shapeId);
		return Response.status(Status.CREATED).entity(gson.toJson(modelElementDetail)).build();
	}

	private ParameterizedSparqlString getInsertContainedModelElementsQuery(String containerInstance, List<String> containedShapes) {

		StringBuilder command = new StringBuilder("INSERT DATA {\n");
		containedShapes.forEach(shape -> {
			command.append(String.format(
					"\t%1$s:%2$s lo:modelingContainerContainsModelingLanguageConstruct %1$s:%3$s .\n",
					MODEL.getPrefix(),
					containerInstance,
					shape));
		});
		command.append("}");

		return new ParameterizedSparqlString(command.toString());
	}

	private ParameterizedSparqlString getDeleteContainedModelElementsQuery(String modelingLanguageConstructInstance) {

		String command = String.format(
				"DELETE\n" +
						"{\n" +
						"\t%1$s:%2$s lo:modelingContainerContainsModelingLanguageConstruct ?o\n" +
						"}\n" +
						"WHERE {\n" +
						"\t%1$s:%2$s lo:modelingContainerContainsModelingLanguageConstruct ?o\n" +
						"}",
				MODEL.getPrefix(),
				modelingLanguageConstructInstance
		);

		return new ParameterizedSparqlString(command);
	}

	private Optional<ParameterizedSparqlString> getInsertModelElementDataQuery(String modelingLanguageConstruct, List<ModelElementAttribute> modelElementAttributes) {

		List<ModelElementAttribute> nonNullAttributes = modelElementAttributes.stream()
				.filter(modelElementAttribute -> modelElementAttribute.getValue() != null && !modelElementAttribute.getValue().isEmpty())
				.collect(Collectors.toList());

		if (nonNullAttributes.isEmpty()) {
			return Optional.empty();
		}

		StringBuilder insertQueryBuilder = new StringBuilder("INSERT DATA { \n");

		nonNullAttributes.forEach(modelElementAttribute -> {
			String line = String.format(
					" %1$s:%2$s %3$s:%4$s %5$s . \n",
					MODEL.getPrefix(),
					modelingLanguageConstruct,
					modelElementAttribute.getRelationPrefix(),
					modelElementAttribute.getRelation(),
					modelElementAttribute.getValue()
			);

			insertQueryBuilder.append(line);
		});

		insertQueryBuilder.append(" } ");

		return Optional.of(new ParameterizedSparqlString(insertQueryBuilder.toString()));
	}

	private ParameterizedSparqlString getDeleteModelElementDataQuery(String modelingLanguageConstruct, List<ModelElementAttribute> attributes) {

		StringBuilder deleteQueryBuilder = new StringBuilder("DELETE { \n");
		StringBuilder whereQueryBuilder = new StringBuilder("WHERE { \n");

		for (int i = 0; i < attributes.size(); i++) {
			ModelElementAttribute modelElementAttribute = attributes.get(i);
			String line = String.format(
					" %1$s:%2$s %3$s:%4$s ?value%5$s . \n",
					MODEL.getPrefix(),
					modelingLanguageConstruct,
					modelElementAttribute.getRelationPrefix(),
					modelElementAttribute.getRelation(),
					i
			);
			deleteQueryBuilder.append(line);
			whereQueryBuilder.append(String.format("OPTIONAL { %s } \n", line));
		}

		deleteQueryBuilder.append("}");
		whereQueryBuilder.append("}");

		return new ParameterizedSparqlString(deleteQueryBuilder.toString() + whereQueryBuilder.toString());
	}

	private ParameterizedSparqlString getInsertOptionalShapeDataQuery(String shapeId, ModelElementDetailDto dto) {

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("INSERT DATA {\n");

		if (dto.getNote() != null) {
			queryBuilder.append(String.format(
					"\t%1$s:%2$s %1$s:shapeHasNote \"%3$s\" .\n",
					MODEL.getPrefix(),
					shapeId,
					dto.getNote()));
		}

		if (dto.getLabel() != null) {
			queryBuilder.append(String.format(
					"\t%1$s:%2$s rdfs:label \"%3$s\" .\n",
					MODEL.getPrefix(),
					shapeId,
					dto.getLabel()));
		}

		if (dto.getShapeRepresentsModel() != null) {
			queryBuilder.append(String.format(
					"\t%1$s:%2$s %1$s:shapeRepresentsModel %1$s:%3$s .\n",
					MODEL.getPrefix(),
					shapeId,
					dto.getShapeRepresentsModel()));
		}

		queryBuilder.append("}");

		return new ParameterizedSparqlString(queryBuilder.toString());
	}

	private ParameterizedSparqlString getInsertShapeDataQuery(String shapeId, ModelElementDetailDto dto) {
		String command = String.format(
				"INSERT DATA {\n" +
						"\t%1$s:%2$s %1$s:shapePositionsOnCoordinateX %3$s .\n" +
						"\t%1$s:%2$s %1$s:shapePositionsOnCoordinateY %4$s .\n" +
						"\t%1$s:%2$s %1$s:shapeHasHeight %5$s .\n" +
						"\t%1$s:%2$s %1$s:shapeHasWidth %6$s .\n" +
						"\t%1$s:%2$s %1$s:shapeInstantiatesPaletteConstruct %7$s .\n" +
						"\t%1$s:%2$s %1$s:shapeVisualisesConceptualElement %1$s:%8$s .\n" +
						"} ",
				MODEL.getPrefix(),
				shapeId,
				dto.getX(),
				dto.getY(),
				dto.getHeight(),
				dto.getWidth(),
				dto.getPaletteConstruct(),
				dto.getModelingLanguageConstructInstance()
		);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("INSERT DATA {\n");

		if (dto.getNote() != null) {
			queryBuilder.append(String.format(
					"\t%1$s:%2$s %1$s:shapeHasNote \"%3$s\" .\n",
					MODEL.getPrefix(),
					shapeId,
					dto.getNote()));
		}

		if (dto.getLabel() != null) {
			queryBuilder.append(String.format(
					"\t%1$s:%2$s rdfs:label \"%3$s\" .\n",
					MODEL.getPrefix(),
					shapeId,
					dto.getLabel()));
		}

		if (dto.getShapeRepresentsModel() != null) {
			queryBuilder.append(String.format(
					"\t%1$s:%2$s %1$s:shapeRepresentsModel %1$s:%3$s .\n",
					MODEL.getPrefix(),
					shapeId,
					dto.getShapeRepresentsModel()));
		}

		queryBuilder.append("}");

		return new ParameterizedSparqlString(command);
	}

	private ParameterizedSparqlString getDeleteShapeQuery(String shapeId) {
		String command = String.format(
				"DELETE {\n" +
						"\t%1$s:%2$s rdf:type %1$s:Shape .\n" +
						"\t%1$s:%2$s %1$s:shapePositionsOnCoordinateX ?x .\n" +
						"\t%1$s:%2$s %1$s:shapePositionsOnCoordinateY ?y .\n" +
						"\t%1$s:%2$s %1$s:shapeHasHeight ?h .\n" +
						"\t%1$s:%2$s %1$s:shapeHasWidth ?w .\n" +
						"\t%1$s:%2$s %1$s:shapeInstantiatesPaletteConstruct ?po .\n" +
						"\t%1$s:%2$s %1$s:shapeVisualisesConceptualElement ?mlo .\n" +
						"\t%1$s:%2$s %1$s:shapeHasNote ?note .\n" +
						"\t%1$s:%2$s rdfs:label ?label .\n" +
						"\t%1$s:%2$s %1$s:shapeRepresentsModel ?model .\n" +
						"\t?parentModel %1$s:modelHasShape %1$s:%2$s .\n" +
						"} WHERE {\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapePositionsOnCoordinateX ?x }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapePositionsOnCoordinateY ?y }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapeHasHeight ?h }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapeHasWidth ?w }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapeInstantiatesPaletteConstruct ?po }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapeVisualisesConceptualElement ?mlo }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapeHasNote ?note }\n" +
						"\tOPTIONAL { %1$s:%2$s rdfs:label ?label }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapeRepresentsModel ?model }\n" +
						"\t?parentModel %1$s:modelHasShape %1$s:%2$s .\n" +
						"} ",
				MODEL.getPrefix(),
				shapeId
		);

		return new ParameterizedSparqlString(command);
	}

	private ParameterizedSparqlString getDeleteShapeDataQuery(String shapeId) {
		String command = String.format(
				"DELETE {\n" +
						"\t%1$s:%2$s %1$s:shapePositionsOnCoordinateX ?x .\n" +
						"\t%1$s:%2$s %1$s:shapePositionsOnCoordinateY ?y .\n" +
						"\t%1$s:%2$s %1$s:shapeHasHeight ?h .\n" +
						"\t%1$s:%2$s %1$s:shapeHasWidth ?w .\n" +
						"\t%1$s:%2$s %1$s:shapeInstantiatesPaletteConstruct ?po .\n" +
						"\t%1$s:%2$s %1$s:shapeVisualisesConceptualElement ?mlo .\n" +
						"\t%1$s:%2$s %1$s:shapeHasNote ?note .\n" +
						"\t%1$s:%2$s rdfs:label ?label .\n" +
						"\t%1$s:%2$s %1$s:shapeRepresentsModel ?model .\n" +
						"} WHERE {\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapePositionsOnCoordinateX ?x }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapePositionsOnCoordinateY ?y }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapeHasHeight ?h }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapeHasWidth ?w }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapeInstantiatesPaletteConstruct ?po }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapeVisualisesConceptualElement ?mlo }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapeHasNote ?note }\n" +
						"\tOPTIONAL { %1$s:%2$s rdfs:label ?label }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:shapeRepresentsModel ?model }\n" +
						"} ",
				MODEL.getPrefix(),
				shapeId
		);

		return new ParameterizedSparqlString(command);
	}

	private ParameterizedSparqlString getShapeCreationQuery(ModelElementCreationDto modelElementCreationDto, String modelId, String elementId) {

		String command = String.format(
				"INSERT DATA {\n" +
						"	%1$s:%2$s rdf:type %1$s:Shape .\n" +
						"	%1$s:%2$s %1$s:shapePositionsOnCoordinateX %6$s .\n" +
						"	%1$s:%2$s %1$s:shapePositionsOnCoordinateY %7$s .\n" +
						"	%1$s:%2$s %1$s:shapeHasWidth %8$s .\n" +
						"	%1$s:%2$s %1$s:shapeHasHeight %9$s .\n" +
						"	%1$s:%2$s %1$s:shapeInstantiatesPaletteConstruct %5$s .\n" +
						"	%1$s:%2$s %1$s:shapeVisualisesConceptualElement %1$s:%4$s .\n" +
						"	%1$s:%3$s %1$s:modelHasShape %1$s:%2$s .\n",
				MODEL.getPrefix(),
				modelElementCreationDto.getUuid(),
				modelId,
				elementId,
				modelElementCreationDto.getPaletteConstruct(),
				modelElementCreationDto.getX(),
				modelElementCreationDto.getY(),
				modelElementCreationDto.getW(),
				modelElementCreationDto.getH()
		);

		StringBuilder commandBuilder = new StringBuilder(command);

		if (modelElementCreationDto.getNote() != null) {
			commandBuilder.append(String.format("	%1$s:%2$s %1$s:shapeHasNote \"%3$s\" .\n",
					MODEL.getPrefix(),
					modelElementCreationDto.getUuid(),
					modelElementCreationDto.getNote()));
		}

		if (modelElementCreationDto.getShapeRepresentsModel() != null) {
			commandBuilder.append(String.format("	%1$s:%2$s %1$s:shapeRepresentsModel %1$s:%3$s .\n",
					MODEL.getPrefix(),
					modelElementCreationDto.getUuid(),
					modelElementCreationDto.getShapeRepresentsModel()));
		}

		if (modelElementCreationDto.getLabel() != null) {
			commandBuilder.append(String.format("	%1$s:%2$s rdfs:label \"%3$s\" .\n",
					MODEL.getPrefix(),
					modelElementCreationDto.getUuid(),
					modelElementCreationDto.getLabel()));
		}

		commandBuilder.append("}");

		return new ParameterizedSparqlString(commandBuilder.toString());
	}

	private String getConnectionCreationCommand(ConnectionCreationDto connectionCreationDto, String modelId, String id, String elementId) {

		String instanceCreationBit = "	%7$s:%1$s rdf:type ?type .\n";
		String classCreationBit = "	%7$s:%1$s rdf:type owl:Class .\n" +
				"	%7$s:%1$s rdfs:subClassOf ?type .\n";

		String creationBit = connectionCreationDto.getInstantiationType() == InstantiationTargetType.Class ? classCreationBit : instanceCreationBit;

		return String.format(
				"INSERT {\n" +
						creationBit +
						"	%7$s:%1$s rdf:type %7$s:ConceptualElement .\n" +
						"	%7$s:%1$s lo:elementIsMappedWithDOConcept ?concept .\n" +
						"	%7$s:%1$s lo:modelingRelationHasSourceModelingElement ?fromInstance .\n" +
						"	%7$s:%1$s lo:modelingRelationHasTargetModelingElement ?toInstance .\n" +
						"	%7$s:%2$s rdf:type %7$s:Shape .\n" +
						"	%7$s:%2$s %7$s:shapePositionsOnCoordinateX %5$s .\n" +
						"	%7$s:%2$s %7$s:shapePositionsOnCoordinateY %6$s .\n" +
						"	%7$s:%2$s %7$s:shapeHasHeight ?height .\n" +
						"	%7$s:%2$s %7$s:shapeHasWidth ?width .\n" +
						"	%7$s:%2$s %7$s:shapeInstantiatesPaletteConstruct po:%3$s .\n" +
						"	%7$s:%2$s %7$s:shapeVisualisesConceptualElement %7$s:%1$s .\n" +
						"	%7$s:%4$s %7$s:modelHasShape %7$s:%2$s .\n" +
						"}" +
						"WHERE {" +
						"	po:%3$s po:paletteConstructIsRelatedToModelingLanguageConstruct ?type .\n" +
						"	po:%3$s po:paletteConstructHasHeight ?height .\n" +
						"	po:%3$s po:paletteConstructHasWidth ?width .\n" +
						"   %7$s:%8$s rdf:type %7$s:Shape . \n" +
						"   %7$s:%9$s rdf:type %7$s:Shape . \n" +
						"   %7$s:%8$s %7$s:shapeVisualisesConceptualElement ?fromInstance . \n" +
						"   %7$s:%9$s %7$s:shapeVisualisesConceptualElement ?toInstance . \n" +
						"	OPTIONAL { ?type lo:elementIsMappedWithDOConcept ?concept }\n" +
						"}",
				elementId,
				id,
				connectionCreationDto.getPaletteConstruct(),
				modelId,
				connectionCreationDto.getX(),
				connectionCreationDto.getY(),
				MODEL.getPrefix(),
				connectionCreationDto.getFrom(),
				connectionCreationDto.getTo()
		);
	}

	private Optional<String> getMappedModelingLanguageConstruct(String paletteConstruct) {
		String command = String.format(
				"SELECT ?mlo " +
						"WHERE " +
						"{ " +
						"	po:%s po:paletteConstructIsRelatedToModelingLanguageConstruct ?mlo" +
						"}",
				paletteConstruct);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		if (!resultSet.hasNext()) {
			return Optional.empty();
		}

		return Optional.ofNullable(extractIdFrom(resultSet.next(), "?mlo"));
	}

	@POST
	@Path("model-elements/search")
	public Response getModelElements(String json) {

		ModellingLanguageConstructInstancesRequest dto = gson.fromJson(json, ModellingLanguageConstructInstancesRequest.class);

		String command = String.format(
				"SELECT ?model ?shape ?instance \n" +
						"WHERE { \n" +
						"{ \n" +
						"    ?shape %2$s:shapeInstantiatesPaletteConstruct <%1$s> .\n" +
						"    ?shape %2$s:shapeVisualisesConceptualElement ?instance .  \n" +
						"    ?model %2$s:modelHasShape ?shape  \n" +
						"  } " +
						"  UNION\n" +
						"  {\n" +
						"    <%1$s> po:paletteConstructIsRelatedToModelingLanguageConstruct ?mloConstruct . \n" +
						"\n" +
						"    ?instance rdf:type mod:ConceptualElement . \n" +
						"    ?instance rdf:type ?mloConstruct . \n" +
						"    \n" +
						"    ?shape %2$s:shapeVisualisesConceptualElement ?instance . \n" +
						"    ?model %2$s:modelHasShape ?shape \n" +
						"  }\n" +
						"  UNION\n" +
						"  {\n" +
						"    <%1$s> po:paletteConstructIsRelatedToModelingLanguageConstruct ?mloConstruct . \n" +
						"    \n" +
						"    ?instance rdf:type mod:ConceptualElement . \n" +
						"    ?instance rdfs:subClassOf ?mloConstruct . \n" +
						"    \n" +
						"    ?shape %2$s:shapeVisualisesConceptualElement ?instance . \n" +
						"    ?model %2$s:modelHasShape ?shape \n" +
						"  }\n" +
						"  \n" +
						"}",
				dto.getId(),
				MODEL.getPrefix()
		);
		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		List<ModellingLanguageConstructInstance> instances = new ArrayList<>();

		while (resultSet.hasNext()) {
			QuerySolution next = resultSet.next();
			String modelId = extractIdFrom(next, "?model");
			String shapeId = extractIdFrom(next, "?shape");
			String instanceId = extractIdFrom(next, "?instance");

			instances.add(new ModellingLanguageConstructInstance(modelId, shapeId, instanceId));
		}

		String payload = gson.toJson(instances);

		return Response.status(Status.OK).entity(payload).build();
	}

	/**
	 * Utility endpoint which can be utilised to find conceptual elements which are not visualised in a model
	 *
	 * @param filter
	 * @return
	 * @throws MethodNotSupportedException
	 */
	@GET
	@Path("/model-element")
	public Response getModelElementInstance(@QueryParam("filter") String filter) throws MethodNotSupportedException {

		if (!"HIDDEN".equals(filter)) {
			throw new MethodNotSupportedException("only searching for HIDDEN elements is allowed right now!");
		}

		String command = String.format(
				"SELECT ?instance ?relation ?object\n" +
						"WHERE\n" +
						"{\n" +
						"\t{\n" +
						"\t\t?instance rdf:type %1$s:ConceptualElement .\n" +
						"\t\t?instance ?relation ?object\n" +
						"\n" +
						"\t\tFILTER NOT EXISTS\n" +
						"\t\t{\n" +
						"\t\t\t?shape %1$s:shapeVisualisesConceptualElement ?instance\n" +
						"\t\t}\n" +
						"\t}\n" +
						"\tUNION\n" +
						"\t{\n" +
						"\t\t?instance rdf:type %1$s:ConceptualElement .\n" +
						"\t\t?subject ?relation ?instance\n" +
						"\n" +
						"\t\tFILTER NOT EXISTS\n" +
						"\t\t{\n" +
						"\t\t\t?shape %1$s:shapeVisualisesConceptualElement ?instance\n" +
						"\t\t}\n" +
						"\t}\n" +
						"}\n",
				MODEL.getPrefix()
		);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		Map<String, Map<String, String>> attributes = new HashMap<>();

		while (resultSet.hasNext()) {
			QuerySolution solution = resultSet.next();

			String instance = extractIdFrom(solution, "?instance");
			attributes.putIfAbsent(instance, new HashMap<>());

			String relation = extractIdFrom(solution, "?relation");
			String value = extractValueFrom(solution, "?object");

			attributes.get(instance).putIfAbsent(relation, value);
		}

		String payload = gson.toJson(attributes);

		return Response.status(Status.OK).entity(payload).build();
	}

	@DELETE
	@Path("/model-element/{id}")
	public Response deleteModelElementInstance(@PathParam("id") String id) {

		String deleteElementCommand = String.format(
				"DELETE {\n" +
						"\t%1$s:%2$s ?rOutgoing ?o .\n" +
						"    ?s ?rIncoming %1$s:%2$s .\n" +
						"}\n" +
						"WHERE {\n" +
						"\t%1$s:%2$s ?rOutgoing ?o .\n" +
						"    OPTIONAL { ?s ?rIncoming %1$s:%2$s } \n" +
						"}",
				MODEL.getPrefix(),
				id
		);

		ParameterizedSparqlString deleteElement = new ParameterizedSparqlString(deleteElementCommand);
		ontology.insertQuery(deleteElement);

		return Response.status(Status.OK).build();
	}

	@GET
	@Path("/arrow-structures")
	public Response getArrowDefinitions() {

		// strokes are taken from https://gojs.net/latest/samples/relationships.html

		List<String> arrowHeads = getArrowHeads();
		List<String> arrowStrokes = getArrowStrokes();

		String payload = gson.toJson(new ArrowStructuresDto(arrowHeads, arrowStrokes));

		return Response.ok().entity(payload).build();
	}

	private List<String> getArrowHeads() {
		String command = "SELECT ?label\n" +
				"WHERE {\n" +
				"\t?arrowHead rdf:type po:ArrowHead .\n" +
				"\t?arrowHead rdfs:label ?label\n" +
				"}\n";

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		List<String> arrowHeads = new ArrayList<>();

		while (resultSet.hasNext()) {
			String label = extractValueFrom(resultSet.next(), "?label");
			arrowHeads.add(label);
		}
		return arrowHeads;
	}

	private List<String> getArrowStrokes() {
		String command = "SELECT ?label\n" +
				"WHERE {\n" +
				"\t?arrowHead rdf:type po:ArrowStroke .\n" +
				"\t?arrowHead rdfs:label ?label\n" +
				"}\n";

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		List<String> strokes = new ArrayList<>();

		while (resultSet.hasNext()) {
			String label = extractValueFrom(resultSet.next(), "?label");
			strokes.add(label);
		}
		return strokes;
	}

	@GET
	@Path("relations/{name}/options")
	public Response getOptionsForRelation(@PathParam("name") String name) {

		String typeCommand = String.format(
				"SELECT ?range\n" +
						"WHERE {\n" +
						"\t%1$s rdf:type owl:DatatypeProperty .\n" +
						"\t%1$s rdfs:range ?range \n" +
						"}",
				name
		);

		ParameterizedSparqlString typeQuery = new ParameterizedSparqlString(typeCommand);
		ResultSet typeResultSet = ontology.query(typeQuery).execSelect();

		if (typeResultSet.hasNext()) {
			String range = extractNamespaceAndIdFrom(typeResultSet.next(), "?range");
			String payload = gson.toJson(new Options(null, null, true, range));
			return Response.ok(payload).build();
		}

		String command = String.format(
				"SELECT ?class ?instance\n" +
						"WHERE { \n" +
						"\t%1$s rdfs:range ?range .\n" +
						"\t?class rdfs:subClassOf* ?range .\n" +
						"\tOPTIONAL {\t?instance rdf:type ?class }\n" +
						"}",
				name);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		Set<String> classes = new HashSet<>();
		Set<String> instances = new HashSet<>();

		while (resultSet.hasNext()) {
			QuerySolution next = resultSet.next();
			String clazz = extractValueFrom(next, "?class");
			String instance = extractValueFrom(next, "?instance");

			if (clazz != null) {
				String prefix = GlobalVariables.getNamespaceMap().get(clazz.split("#")[0]);
				classes.add(prefix + ":" + clazz.split("#")[1]);
			}

			if (instance != null) {
				String prefix = GlobalVariables.getNamespaceMap().get(instance.split("#")[0]);
				instances.add(prefix + ":" + instance.split("#")[1]);
			}
		}

		String payload = gson.toJson(new Options(instances, classes, false, null));

		return Response.ok(payload).build();
	}

	@GET
	@Path("/getModelingLanguages")
	public Response getModelingLanguages() {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested modeling languages");
		System.out.println("####################<end>####################");
		ArrayList<ModelingLanguage> all_modeling_languages = new ArrayList<ModelingLanguage>();

		try {
			all_modeling_languages = queryAllModelingLangugages();

			if (debug_properties) {
				for (int index = 0; index < all_modeling_languages.size(); index++) {
					System.out.println("Langugage " + index + ": ");
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(all_modeling_languages);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " + json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}

	private ArrayList<ModelingLanguage> queryAllModelingLangugages() throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<ModelingLanguage> result = new ArrayList<ModelingLanguage>();

		queryStr.append("SELECT ?element ?label ?hasModelingView ?viewIsPartOfModelingLanguage ?type WHERE {");
		queryStr.append("?element rdf:type ?type . FILTER(?type IN (lo:ModelingLanguage)) .");
		queryStr.append("?element rdfs:label ?label . ");
		queryStr.append("OPTIONAL {?element lo:hasModelingView ?hasModelingView }.");    //not needed, remove
		queryStr.append("OPTIONAL {?element lo:viewIsPartOfModelingLanguage ?viewIsPartOfModelingLanguage }.");//not needed, remove

		queryStr.append("}");
		//queryStr.append("ORDER BY ?domain ?field");
		
		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();

		if (results.hasNext()) {
			while (results.hasNext()) {
				ModelingLanguage tempModelingLanguage = new ModelingLanguage();

				QuerySolution soln = results.next();
				String[] id = (soln.get("?element").toString()).split("#"); //eg. http://fhnw.ch/modelingEnvironment/LanguageOntology#DMN_1.1
				String prefix = GlobalVariables.getNamespaceMap().get(id[0]); //eg. lo
				String simpleId = prefix + ":" + id[1]; //eg. lo:DMN_1.1
				tempModelingLanguage.setId(simpleId);
				tempModelingLanguage.setLabel(soln.get("?label").toString());
				if (soln.get("?hasModelingView") != null) {
					tempModelingLanguage.setHasModelingView(FormatConverter.ParseOntologyBoolean(soln.get("?hasModelingView").toString()));
				}
				if (soln.get("?viewIsPartOfModelingLanguage") != null) {
					tempModelingLanguage.setViewIsPartOfModelingLanguage(soln.get("?viewIsPartOfModelingLanguage").toString());
				}


				result.add(tempModelingLanguage);
			}
		}
		qexec.close();
		return result;
	}

	@GET
	@Path("/getModelingViews/{langId}")
	public Response getModelingViews(@PathParam("langId") String langId) {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested modeling languages");
		System.out.println("####################<end>####################");
		ArrayList<ModelingView> all_modeling_views = new ArrayList<ModelingView>();

		try {
			all_modeling_views = queryAllModelingViews(langId);

			if (debug_properties) {
				for (int index = 0; index < all_modeling_views.size(); index++) {
					System.out.println("View " + index + ": ");
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(all_modeling_views);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " + json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}

	private ArrayList<ModelingView> queryAllModelingViews(String langId) throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<ModelingView> result = new ArrayList<ModelingView>();

		queryStr.append("SELECT ?element ?label ?isMainModelingView ?viewIsPartOfModelingLanguage ?type WHERE {");
		//queryStr.append("?element rdf:type ?type . FILTER(?type IN (lo:ModelingView)) .");
		queryStr.append("?element rdfs:label ?label .");
		queryStr.append("?element lo:viewIsPartOfModelingLanguage " + langId + " .");
		queryStr.append("?element lo:isMainModelingView ?isMainModelingView .");

		queryStr.append("}");
		//queryStr.append("ORDER BY ?domain ?field");
		
		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();

		if (results.hasNext()) {
			while (results.hasNext()) {
				ModelingView tempModelingView = new ModelingView();

				QuerySolution soln = results.next();
				String[] id = (soln.get("?element").toString()).split("#"); //eg. http://fhnw.ch/modelingEnvironment/LanguageOntology#DMN_1.1
				String prefix = GlobalVariables.getNamespaceMap().get(id[0]); //eg. lo
				String simpleId = prefix + ":" + id[1]; //eg. lo:DMN_1.1
				tempModelingView.setId(simpleId);
				tempModelingView.setLabel(soln.get("?label").toString());
				if (soln.get("?isMainModelingView") != null) {
					tempModelingView.setMainModelingView(FormatConverter.ParseOntologyBoolean(soln.get("?isMainModelingView").toString()));
				}
				if (soln.get("?viewIsPartOfModelingLanguage") != null) {
					tempModelingView.setViewIsPartOfModelingLanguage(soln.get("?viewIsPartOfModelingLanguage").toString());
				}


				result.add(tempModelingView);
			}
		}
		qexec.close();
		return result;
	}

	@GET
	@Path("/getPaletteElements")
	public Response getPaletteElements() {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested palette elements");
		System.out.println("####################<end>####################");
		ArrayList<PaletteElement> all_palette_elements = new ArrayList<PaletteElement>();

		try {
			all_palette_elements = queryAllPaletteElements();

			if (debug_properties) {
				for (int index = 0; index < all_palette_elements.size(); index++) {
					System.out.println("Element " + index + ": ");
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(addChildElements(all_palette_elements));
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " + json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}

	private ArrayList<PaletteElement> queryAllPaletteElements() throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<PaletteElement> result = new ArrayList<PaletteElement>();

		queryStr.append("SELECT ?element ?label ?representedClass ?hidden ?category ?categoryLabel ?parent ?backgroundColor ?height ?iconPosition ?iconURL ?imageURL ?labelPosition ?shape ?thumbnailURL ?usesImage ?width ?borderColor ?borderType ?borderThickness ?comment ?type ?fromArrow ?toArrow ?arrowStroke WHERE {");
		queryStr.append("?element rdf:type ?type . FILTER(?type IN (po:PaletteElement, po:PaletteConnector)) .");
		queryStr.append("?element rdfs:label ?label .");
		queryStr.append("?element po:paletteConstructIsRelatedToModelingLanguageConstruct ?representedClass .");
		//queryStr.append("?element po:languageElementIsRelatedToDomainElement ?representedDomainClasses ."); //not sure how to read multiple values
		queryStr.append("?element po:paletteConstructIsHiddenFromPalette ?hidden .");
		queryStr.append("?element po:paletteConstructIsGroupedInPaletteCategory ?category .");
		queryStr.append("?category rdfs:label ?categoryLabel .");
		//queryStr.append("?element po:paletteCategoryBelongsToModelingView " + viewId + " .");
		//queryStr.append("?element po:paletteElementUsesImage ?usesImage ."); //currently not used as every element uses an image

		queryStr.append("OPTIONAL{ ?element po:paletteElementBackgroundColor ?backgroundColor }.");
		queryStr.append("OPTIONAL{ ?element po:paletteConstructHasHeight ?height }.");
		//queryStr.append("OPTIONAL{ ?element po:paletteElementIconPosition ?iconPosition }."); //future use
		//queryStr.append("OPTIONAL{ ?element po:paletteElementIconURL ?iconURL}."); //future use
		queryStr.append("OPTIONAL{ ?element po:paletteConstructHasModelImage ?imageURL }.");
		//queryStr.append("OPTIONAL{ ?element po:paletteElementLabelPosition ?labelPosition }."); //future use
		queryStr.append("OPTIONAL{ ?element po:paletteElementShape ?shape }.");
		queryStr.append("OPTIONAL{ ?element po:paletteConstructHasPaletteThumbnail ?thumbnailURL }.");
		queryStr.append("OPTIONAL{ ?element po:paletteConstructHasWidth ?width }.");
		//queryStr.append("OPTIONAL{ ?element po:paletteConstructBelongsToModelingView ?view }.");
		//queryStr.append("OPTIONAL{ ?element po:paletteElementBorderColor ?borderColor }."); //future use
		//queryStr.append("OPTIONAL{ ?element po:paletteElementBorderThickness ?borderThickness }."); //future use
		//queryStr.append("OPTIONAL{ ?element po:paletteElementBorderType ?borderType }."); //future use
		queryStr.append("OPTIONAL{ ?representedClass rdfs:comment ?comment }.");
		queryStr.append("OPTIONAL{ ?element po:paletteConstructHasParentPaletteConstruct ?parent }.");

		queryStr.append("OPTIONAL{ ?element po:paletteConnectorConfiguresFromArrowHead ?fromArrow }.");
		queryStr.append("OPTIONAL{ ?element po:paletteConnectorConfiguresToArrowHead ?toArrow }.");
		queryStr.append("OPTIONAL{ ?element po:paletteConnectorConfiguresArrowStroke ?arrowStroke }.");

		queryStr.append("}");
		//queryStr.append("ORDER BY ?domain ?field");
		
		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();

		if (results.hasNext()) {
			while (results.hasNext()) {
				PaletteElement tempPaletteElement = new PaletteElement();

				QuerySolution soln = results.next();
				tempPaletteElement.setId(soln.get("?element").toString());
				tempPaletteElement.setLabel(soln.get("?label").toString());
				tempPaletteElement.setRepresentedLanguageClass(soln.get("?representedClass").toString());
				tempPaletteElement.setHiddenFromPalette(FormatConverter.ParseOntologyBoolean(soln.get("?hidden").toString()));
				tempPaletteElement.setPaletteCategory(soln.get("?category").toString());
				//tempPaletteElement.setUsesImage(FormatConverter.ParseOntologyBoolean(soln.get("?usesImage").toString()));

				if (soln.get("?backgroundColor") != null) {
					tempPaletteElement.setBackgroundColor(soln.get("?backgroundColor").toString());
				}

				if (soln.get("?height") != null) {
					tempPaletteElement.setHeight(FormatConverter.ParseOntologyInteger(soln.get("?height").toString()));
				}
				/*if (soln.get("?iconPosition") != null){
					tempPaletteElement.setIconPosition(soln.get("?iconPosition").toString());
				}
				if (soln.get("?iconURL") != null){
					tempPaletteElement.setIconURL(soln.get("?iconURL").toString());
				}*/
				if (soln.get("?imageURL") != null) {
					tempPaletteElement.setImageURL(soln.get("?imageURL").toString());
				}
				/*if (soln.get("?labelPosition") != null){
					tempPaletteElement.setLabelPosition(soln.get("?labelPosition").toString());
				}*/
				if (soln.get("?shape") != null) {
					tempPaletteElement.setShape(soln.get("?shape").toString());
				}
				if (soln.get("?thumbnailURL") != null) {
					tempPaletteElement.setThumbnailURL(soln.get("?thumbnailURL").toString());
				}
				if (soln.get("?width") != null) {
					tempPaletteElement.setWidth(FormatConverter.ParseOntologyInteger(soln.get("?width").toString()));
				}
				if (soln.get("?categoryLabel") != null) {
					tempPaletteElement.setCategoryLabel(soln.get("?categoryLabel").toString());
				}
				/*if (soln.get("?borderColor") != null){
					tempPaletteElement.setBorderColor(soln.get("?borderColor").toString());
				}
				if (soln.get("?borderThickness") != null){
					tempPaletteElement.setBorderThickness(soln.get("?borderThickness").toString());
				}
				if (soln.get("?borderType") != null){
					tempPaletteElement.setBorderType(soln.get("?borderType").toString());
				}*/
				if (soln.get("?comment") != null) {
					tempPaletteElement.setComment(soln.get("?comment").toString());
				}
				if (soln.get("?parent") != null) {
					tempPaletteElement.setParentElement(soln.get("?parent").toString());
					//Read properties of the parent element here
				}

				if (soln.get("?arrowStroke") != null) {
					tempPaletteElement.setArrowStroke(extractIdFrom(soln, "?arrowStroke"));
				}

				if (soln.get("?toArrow") != null) {
					tempPaletteElement.setToArrow(extractIdFrom(soln, "?toArrow"));
				}

				if (soln.get("?fromArrow") != null) {
					tempPaletteElement.setFromArrow(extractIdFrom(soln, "?fromArrow"));
				}

				String type = extractIdFrom(soln, "?type");
				tempPaletteElement.setType(type);

				String prefix = GlobalVariables.getNamespaceMap().get(tempPaletteElement.getRepresentedLanguageClass().split("#")[0]);
				tempPaletteElement.setLanguagePrefix(prefix);
				//System.out.println("language class: "+tempPaletteElement.getRepresentedLanguageClass());
				//System.out.println("prefix: "+prefix);

				result.add(tempPaletteElement);
			}
		}
		qexec.close();
		return result;
	}

	@GET
	@Path("/getPaletteCategories/{viewId}")
	public Response getPaletteCategories(@PathParam("viewId") String viewId) {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested palette categories");
		System.out.println("####################<end>####################");
		ArrayList<PaletteCategory> all_palette_categories = new ArrayList<PaletteCategory>();

		try {
			all_palette_categories = queryAllPaletteCategories(viewId);

			if (debug_properties) {
				for (int index = 0; index < all_palette_categories.size(); index++) {
					System.out.println("Category " + index + ": ");
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(all_palette_categories);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " + json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}

	private ArrayList<PaletteCategory> queryAllPaletteCategories(String viewId) throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<PaletteCategory> result = new ArrayList<PaletteCategory>();

		queryStr.append("SELECT ?category ?label ?orderNumber ?hidden WHERE {");
		queryStr.append("?category rdf:type* po:PaletteCategory .");
		queryStr.append("?category rdfs:label ?label . ");
		queryStr.append("?category po:paletteCategoryIsShownInModelingView " + viewId + " . ");
		queryStr.append("OPTIONAL {?category po:paletteCategoryOrderNumber ?orderNumber . }");
		queryStr.append("OPTIONAL {?category po:hiddenFromPalette ?hidden . }");

		queryStr.append("}");
		queryStr.append("ORDER BY ?orderNumber");
		
		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();

		if (results.hasNext()) {
			while (results.hasNext()) {
				PaletteCategory tempPaletteCategory = new PaletteCategory();

				QuerySolution soln = results.next();
				String categoryURI = soln.get("?category").toString();
				tempPaletteCategory.setId(categoryURI);
				String idSuffix = categoryURI.split("#")[1];
				System.out.println("idSuffix: " + idSuffix);
				tempPaletteCategory.setIdSuffix(idSuffix);
				tempPaletteCategory.setLabel(soln.get("?label").toString());

				if (soln.get("?orderNumber") != null) {
					tempPaletteCategory.setOrderNumber(FormatConverter.ParseOntologyInteger(soln.get("?orderNumber").toString()));
				}
				if (soln.get("?hidden") != null) {
					tempPaletteCategory.setHiddenFromPalette((FormatConverter.ParseOntologyBoolean(soln.get("?hidden").toString())));
				}

				result.add(tempPaletteCategory);
			}
		}
		qexec.close();
		return result;
	}


	private ArrayList<PaletteElement> addChildElements(ArrayList<PaletteElement> all_palette_elements) {
		ArrayList<PaletteElement> parentList = new ArrayList<PaletteElement>();
		//System.out.println("pre: " + all_palette_elements.size());
		for (int i = 0; i < all_palette_elements.size(); i++) {
			if (all_palette_elements.get(i).getParentElement() == null) {
				parentList.add(all_palette_elements.get(i));
			}
		}
		//System.out.println("post: " + all_palette_elements.size());
		//System.out.println("Number of parents: " + parentList.size());
		if (parentList.size() > 0) {
			for (int i = 0; i < parentList.size(); i++) {
				if (parentList.get(i).getChildElements().size() == 0) {
					//System.out.println("=========");
					addChilds(parentList.get(i), all_palette_elements);
					//System.out.println("1. Analysing " + all_palette_elements.get(i).getId());
				}
			}
		}
		return parentList;
	}

	private void addChilds(PaletteElement parent, ArrayList<PaletteElement> list) {
		ArrayList<PaletteElement> childList = getChildren(parent, list);
		for (int i = 0; i < childList.size(); i++) {
			//System.out.println("3. Adding child of " + list.get(i).getId());
			parent.getChildElements().add(childList.get(i));
			addChilds(childList.get(i), list);
		}
	}

	private ArrayList<PaletteElement> getChildren(PaletteElement parent, ArrayList<PaletteElement> list) {
		ArrayList<PaletteElement> result = new ArrayList<PaletteElement>();
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getParentElement() != null &&
					list.get(i).getParentElement().equals(parent.getId()) && parent.getPaletteCategory().equals(list.get(i).getPaletteCategory())) {
				//System.out.println("2. Found a child of " + parent.getId() + " -> " + list.get(i).getId());
				//System.out.println("3. Category of parent: " + parent.getPaletteCategory() + ", of child: " + list.get(i).getPaletteCategory());
				result.add(list.get(i));
			}
		}
		return result;
	}

	@POST
	@Path("/hidePaletteElement")
	public Response hidePalletteElement(String json) {
		System.out.println("/element received: " + json);

		Gson gson = new Gson();
		PaletteElement pElement = gson.fromJson(json, PaletteElement.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		querStr.append("DELETE DATA {");
		System.out.println("    Element ID: " + pElement.getId());
		querStr.append("<" + pElement.getId() + "> po:paletteConstructIsHiddenFromPalette false .");
		querStr.append("}");

		ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();
		querStr1.append("INSERT DATA {");
		System.out.println("    Element ID: " + pElement.getId());
		querStr1.append("<" + pElement.getId() + "> po:paletteConstructIsHiddenFromPalette true .");
		querStr1.append("}");

		ArrayList<ParameterizedSparqlString> queryList = new ArrayList<ParameterizedSparqlString>();
		queryList.add(querStr);
		queryList.add(querStr1);
		
		return Response.status(Status.OK).entity(ontology.insertMultipleQueries(queryList)).build();
	}

	@POST
	@Path("/createPalletteElement")
	public Response insertPalletteElement(String json) {

		System.out.println("/element received: " + json);

		Gson gson = new Gson();
		PaletteElement pElement = gson.fromJson(json, PaletteElement.class);
		//pElement.setClassType("http://fhnw.ch/modelingEnvironment/LanguageOntology#PaletteElement");

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		System.out.println("test: " + pElement.getUuid());
		querStr.append("INSERT DATA {");
		System.out.println("    Element ID: " + pElement.getUuid());
		querStr.append("po:" + pElement.getUuid() + " rdf:type po:" + pElement.getType() + " ;");
		/*System.out.println("    Element Type: " + pElement.getClassType());
			querStr.append("lo:graphicalElementClassType \"" + "<"+pElement.getClassType()+">" +"\" ;");*/
		System.out.println("    Element Label: " + pElement.getLabel());
		querStr.append("rdfs:label \"" + pElement.getLabel() + "\" ;");
		System.out.println("    Element Hidden property: " + pElement.getHiddenFromPalette());
		querStr.append("po:paletteConstructIsHiddenFromPalette " + pElement.getHiddenFromPalette() + " ;");
		System.out.println("    Element Parent: " + pElement.getParentElement());
		querStr.append("po:paletteConstructHasParentPaletteConstruct <" + pElement.getParentElement() + "> ;");
		System.out.println("    Element Category: " + pElement.getPaletteCategory());
		querStr.append("po:paletteConstructIsGroupedInPaletteCategory <" + pElement.getPaletteCategory() + "> ;");
		//System.out.println("    Element UsesImage property: "+ pElement.getUsesImage());
		//querStr.append("po:paletteElementUsesImage \"" + pElement.getUsesImage() +"\" ;"); //currently not used


		if ("PaletteElement".equals(pElement.getType())) {

			System.out.println("    Element Palette Image : " + pElement.getThumbnailURL());
			querStr.append("po:paletteConstructHasPaletteThumbnail \"" + pElement.getThumbnailURL() + "\" ;");
			System.out.println("    Element Canvas Image: " + pElement.getImageURL());
			querStr.append("po:paletteConstructHasModelImage \"" + pElement.getImageURL() + "\" ;");
			System.out.println("    Element Image width: " + pElement.getWidth());
			querStr.append("po:paletteConstructHasWidth " + pElement.getWidth() + " ;");
			System.out.println("    Element Image height: " + pElement.getHeight());
			querStr.append("po:paletteConstructHasHeight " + pElement.getHeight() + " ;");

		} else if ("PaletteConnector".equals(pElement.getType())) {

			System.out.println("    Element From Arrow: " + pElement.getFromArrow());
			querStr.append("po:paletteConnectorConfiguresFromArrowHead po:" + pElement.getFromArrow() + ";");
			System.out.println("    Element To Arrow: " + pElement.getToArrow());
			querStr.append("po:paletteConnectorConfiguresToArrowHead po:" + pElement.getToArrow() + ";");
			System.out.println("    Element Arrow Stroke: " + pElement.getArrowStroke());
			querStr.append("po:paletteConnectorConfiguresArrowStroke po:" + pElement.getArrowStroke() + ";");

		} else {
			System.err.println("Invalid element type: \"" + pElement.getType() + "\"");
			return Response.status(Status.NOT_IMPLEMENTED).build();
		}

		System.out.println("    Element representedLanguage: " + pElement.getRepresentedLanguageClass());
		querStr.append("po:paletteConstructIsRelatedToModelingLanguageConstruct " + pElement.getRepresentedLanguageClass() + " ;");
		//The below property is not needed any more as object properties will be added separately
		/*if(pElement.getRepresentedDomainClass()!=null) {
			querStr.append("po:languageElementIsRelatedToDomainElement ");
			if (pElement.getRepresentedDomainClass().size()!=0) {
				String repDomainClasses = pElement.getRepresentedDomainClass().stream()
						.map(s -> "<" +s+ ">")
						.collect(Collectors.joining(", "));

				System.out.println("Comma separated domain classes: " + repDomainClasses);
				querStr.append(repDomainClasses + " ;");
			}
			else
				querStr.append(" <" + pElement.getRepresentedDomainClass().get(0) +"> ");
		}*/
		querStr.append(" ;");
		/*System.out.println("    Element X Position: "+ pElement.getX());
			querStr.append("lo:graphicalElementX \"" + pElement.getX() +"\" ;");
		System.out.println("    Element Y Position: "+ pElement.getY());
			querStr.append("lo:graphicalElementY \"" + pElement.getY() +"\" ;");*/


		querStr.append("}");
		//Model modelTpl = ModelFactory.createDefaultModel();

		//ontology.insertQuery(querStr);
		querStr.clearParams();
		ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();

		/**
		 * Map multiple domain concepts to the modeling language concept (new element)
		 */
		querStr1.append("INSERT DATA {");
		querStr1.append(pElement.getRepresentedLanguageClass() + " rdf:type rdfs:Class . ");
		querStr1.append(pElement.getRepresentedLanguageClass() + " rdfs:subClassOf <" + pElement.getParentLanguageClass() + "> . ");
		querStr1.append(pElement.getRepresentedLanguageClass() + " rdfs:label \"" + pElement.getLabel() + "\" . ");
		if (pElement.getComment() != null && !"".equals(pElement.getComment()))
			querStr1.append(pElement.getRepresentedLanguageClass() + " rdfs:comment \"" + pElement.getComment() + "\" . ");
		//The below property is not needed any more as object properties will be added separately
		/*if(pElement.getRepresentedDomainClass()!=null && pElement.getRepresentedDomainClass().size()!=0) {
			for(String repDomainClass: pElement.getRepresentedDomainClass()) {
				System.out.println("The selected domain class is : "+repDomainClass);
				if(repDomainClass!=null && !"".equals(repDomainClass))
					querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " po:languageElementIsRelatedToDomainElement <" + repDomainClass + "> . ");
			}
		}*/

		//Verify the below properties!!! They should not be a part of the modeling language construct - verify with Emanuele
		//querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " rdf:type <http://fhnw.ch/modelingEnvironment/PaletteOntology#PaletteElement> . ");
		//querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " po:hasParentPaletteConstruct <http://fhnw.ch/modelingEnvironment/PaletteOntology#" + pElement.getParentElement() +"> . ");
		//querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " po:isRelatedToModelingConstruct " + pElement.getLanguagePrefix() + pElement.getUuid() + " . ");
		querStr1.append("}");
		//querStr1.append(" WHERE { }");

		System.out.println("Create subclass in bpmn Ontology");
		System.out.println(querStr1.toString());
		//ontology.insertQuery(querStr1);
		ArrayList<ParameterizedSparqlString> queryList = new ArrayList<ParameterizedSparqlString>();
		queryList.add(querStr);
		queryList.add(querStr1);

		
		return Response.status(Status.OK).entity(ontology.insertMultipleQueries(queryList)).build();

	}

	@POST
	@Path("/createModelingLanguageSubclasses")
	public Response createModelingLanguageSubclasses(String json) {
		System.out.println("/Element received: " + json);

		Gson gson = new Gson();
		PaletteElement element = gson.fromJson(json, PaletteElement.class);

		ParameterizedSparqlString querStr = null;
		querStr = new ParameterizedSparqlString();
		querStr.append("INSERT DATA {");
		if (element.getLanguageSubclasses() != null && element.getLanguageSubclasses().size() != 0) {
			for (Answer languageSubclass : element.getLanguageSubclasses()) {
				System.out.println("The selected language class is : " + languageSubclass.getLabel());

				//querStr.append("<" + languageSubclass + "> rdf:type rdfs:Class . ");
				String uuid = languageSubclass.getId().split("#")[1];
				//String uuid = element.getUuid();
				System.out.println("uuid: " + uuid);
				querStr.append("<" + languageSubclass.getId() + "> rdfs:subClassOf <" + element.getRepresentedLanguageClass() + "> . ");
				/** The assumption is that the palette element should be already available before creating a language subclass. 
				 * The below clause creates a parent-child relationship between the existing element and the element to be integrated **/
				querStr.append("<http://fhnw.ch/modelingEnvironment/PaletteOntology#" + uuid + "> po:paletteConstructHasParentPaletteConstruct <http://fhnw.ch/modelingEnvironment/PaletteOntology#" + element.getParentElement() + "> . <http://fhnw.ch/modelingEnvironment/PaletteOntology#" + uuid + "> po:paletteConstructIsGroupedInPaletteCategory <" + element.getPaletteCategory() + "> . ");
			}
		}
		querStr.append("}");
		//querStr.append(" WHERE { }");
		
		ontology.insertQuery(querStr);

		return Response.status(Status.OK).entity("{}").build();
	}

	private boolean hasInstantiatedInstances(PaletteElement element) {
		String command = String.format("SELECT *\n" +
						"WHERE {\n" +
						"  {\n" +
						"    ?subject %3$s:shapeInstantiatesPaletteConstruct <%1$s>\n" +
						"  }\n" +
						"  UNION\n" +
						"  {\n" +
						"\t?subject rdf:type <%2$s>\n" +
						"  }\n" +
						"  UNION\n" +
						"  {\n" +
						"    ?subject rdfs:subClassOf <%2$s>\n" +
						"  }\n" +
						"}",
				element.getId(),
				element.getRepresentedLanguageClass(),
				MODEL.getPrefix());

		ParameterizedSparqlString instantiatedQuery = new ParameterizedSparqlString(command);
		
		return ontology.query(instantiatedQuery).execSelect().hasNext();
	}

	@POST
	@Path("/deletePaletteElement")
	public Response deletePaletteElement(String json) {

		System.out.println("/Element received: " + json);

		Gson gson = new Gson();
		PaletteElement element = gson.fromJson(json, PaletteElement.class);

		if (hasInstantiatedInstances(element)) {
			return Response.status(Status.BAD_REQUEST).entity("{}").build();
		}

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();

		/**
		 * Delete a class and all its predicates and values
		 * DELETE
		 * WHERE {bpmn:NewSubprocess ?predicate  ?object .}
		 */

		querStr.append("DELETE ");
		querStr.append("WHERE { <" + element.getRepresentedLanguageClass() + "> ?predicate ?object . } ");
		//querStr.append("INSERT {"+"<"+element.getId()+"> rdfs:label "+element.getLabel());

		System.out.println(querStr.toString());
		//Model modelTpl = ModelFactory.createDefaultModel();
		
		ontology.insertQuery(querStr);

		querStr1.append("DELETE ");
		querStr1.append("WHERE { <" + element.getId() + "> ?predicate ?object . } ");

		System.out.println(querStr1.toString());
		ontology.insertQuery(querStr1);

		return Response.status(Status.OK).entity("{}").build();

	}

	@POST
	@Path("/createCanvasInstance")//Not yet being used in webapp
	public Response insertCanvasInstance(String json) {

		System.out.println("/element received: " + json);

		Gson gson = new Gson();
		GraphicalElement gElement = gson.fromJson(json, GraphicalElement.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		System.out.println("test: " + gElement.getUuid());
		querStr.append("INSERT {");
		System.out.println("    Element ID: " + gElement.getUuid());
		querStr.append("<" + gElement.getUuid() + ">" + " rdf:type " + "<" + gElement.getClassType() + ">" + " ;");
		System.out.println("    Element Type: " + gElement.getClassType());
		querStr.append("po:graphicalElementClassType \"" + "<" + gElement.getClassType() + ">" + "\" ;");
		System.out.println("    Element Label: " + gElement.getLabel());
		querStr.append("rdfs:label \"" + gElement.getLabel() + "\" ;");
		System.out.println("    Element X Position: " + gElement.getX());
		querStr.append("po:graphicalElementX \"" + gElement.getX() + "\" ;");
		System.out.println("    Element Y Position: " + gElement.getY());
		querStr.append("po:graphicalElementY \"" + gElement.getY() + "\" ;");

		querStr.append("}");
		//Model modelTpl = ModelFactory.createDefaultModel();
		
		ontology.insertQuery(querStr);

		return Response.status(Status.OK).entity("{}").build();

	}

	@POST
	@Path("/modifyElement")
	public Response modifyElementLabel(@FormParam("element") String json, @FormParam("modifiedElement") String modifiedJson) {

		System.out.println("/Element received: " + json);
		System.out.println("/Modifed element: " + modifiedJson);

		Gson gson = new Gson();
		PaletteElement element = gson.fromJson(json, PaletteElement.class);
		PaletteElement modifiedElement = gson.fromJson(modifiedJson, PaletteElement.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();

		querStr.append("DELETE DATA { ");
		querStr.append("<" + element.getId() + "> rdfs:label \"" + element.getLabel() + "\" . ");
		querStr.append("<" + element.getId() + "> po:paletteConstructHasModelImage \"" + element.getImageURL() + "\" . ");
		querStr.append("<" + element.getId() + "> po:paletteConstructHasPaletteThumbnail \"" + element.getThumbnailURL() + "\" . ");

		if (element.getToArrow() != null) {
			querStr.append("<" + element.getId() + "> po:paletteConnectorConfiguresToArrowHead po:" + element.getToArrow() + " . ");
		}

		if (element.getFromArrow() != null) {
			querStr.append("<" + element.getId() + "> po:paletteConnectorConfiguresFromArrowHead po:" + element.getFromArrow() + " . ");
		}

		if (element.getArrowStroke() != null) {
			querStr.append("<" + element.getId() + "> po:paletteConnectorConfiguresArrowStroke po:" + element.getArrowStroke() + " . ");
		}

		querStr.append(" }");
		querStr1.append("INSERT DATA { ");
		querStr1.append("<" + element.getId() + "> rdfs:label \"" + modifiedElement.getLabel() + "\" . ");
		querStr1.append("<" + element.getId() + "> po:paletteConstructHasModelImage \"" + modifiedElement.getImageURL() + "\" . ");
		querStr1.append("<" + element.getId() + "> po:paletteConstructHasPaletteThumbnail \"" + modifiedElement.getThumbnailURL() + "\" . ");
		if (modifiedElement.getToArrow() != null) {
			querStr1.append("<" + element.getId() + "> po:paletteConnectorConfiguresToArrowHead po:" + modifiedElement.getToArrow() + " . ");
		}

		if (modifiedElement.getFromArrow() != null) {
			querStr1.append("<" + element.getId() + "> po:paletteConnectorConfiguresFromArrowHead po:" + modifiedElement.getFromArrow() + " . ");
		}

		if (modifiedElement.getArrowStroke() != null) {
			querStr1.append("<" + element.getId() + "> po:paletteConnectorConfiguresArrowStroke po:" + modifiedElement.getArrowStroke() + " . ");
		}
		querStr1.append(" }");

		//Model modelTpl = ModelFactory.createDefaultModel();
		
		ontology.insertQuery(querStr);
		ontology.insertQuery(querStr1);

		//Edit the corresponding modeling language construct
		querStr = new ParameterizedSparqlString();
		querStr1 = new ParameterizedSparqlString();

		querStr.append("DELETE DATA { ");
		querStr.append("<" + element.getRepresentedLanguageClass() + "> rdfs:label \"" + element.getLabel() + "\" . ");
		querStr.append("<" + element.getRepresentedLanguageClass() + "> rdfs:comment \"" + element.getComment() + "\" . ");
		querStr.append(" }");
		querStr1.append("INSERT DATA { ");
		querStr1.append("<" + element.getRepresentedLanguageClass() + "> rdfs:label \"" + modifiedElement.getLabel() + "\" . ");
		querStr1.append("<" + element.getRepresentedLanguageClass() + "> rdfs:comment \"" + modifiedElement.getComment() + "\" . ");
		querStr1.append(" }");

		//Model modelTpl = ModelFactory.createDefaultModel();
		
		ontology.insertQuery(querStr);
		ontology.insertQuery(querStr1);

		return Response.status(Status.OK).entity("{}").build();

	}

	@POST
	@Path("/editDatatypeProperty")
	public Response editDatatypeProperty(@FormParam("property") String json, @FormParam("editedProperty") String modifiedJson) {

		System.out.println("/datatypeProperty received: " + json);
		System.out.println("/Modifed datatypeProperty: " + modifiedJson);

		Gson gson = new Gson();
		DatatypeProperty datatypeProperty = gson.fromJson(json, DatatypeProperty.class);
		DatatypeProperty modifiedDatatypeProperty = gson.fromJson(modifiedJson, DatatypeProperty.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();

		querStr.append("DELETE DATA { ");
		//querStr.append(datatypeProperty.getId() + " rdf:type owl:DataTypeProperty .");
		querStr.append("<" + datatypeProperty.getId() + "> rdfs:label \"" + datatypeProperty.getLabel() + "\" . ");
		querStr.append("<" + datatypeProperty.getId() + "> rdfs:range " + datatypeProperty.getRange() + " . ");
		querStr.append("<" + datatypeProperty.getId() + "> " + MODEL.getPrefix() + ":propertyIsShownInModel " + datatypeProperty.isAvailableToModel() + " . ");
		querStr.append(" }");
		querStr1.append("INSERT DATA { ");
		//querStr1.append(datatypeProperty.getId() + " rdf:type owl:DataTypeProperty .");
		querStr1.append("<" + datatypeProperty.getId() + "> rdfs:label \"" + modifiedDatatypeProperty.getLabel() + "\" . ");
		querStr1.append("<" + datatypeProperty.getId() + "> rdfs:range " + modifiedDatatypeProperty.getRange() + " . ");
		querStr1.append("<" + datatypeProperty.getId() + "> " + MODEL.getPrefix() + ":propertyIsShownInModel " + modifiedDatatypeProperty.isAvailableToModel() + " . ");
		querStr1.append(" }");

		//Model modelTpl = ModelFactory.createDefaultModel();
		
		ontology.insertQuery(querStr);
		ontology.insertQuery(querStr1);

		return Response.status(Status.OK).entity("{}").build();

	}

	@POST
	@Path("/editObjectProperty")
	public Response editObjectProperty(@FormParam("property") String json, @FormParam("editedProperty") String modifiedJson) {

		System.out.println("/objectProperty received: " + json);
		System.out.println("/Modifed objectProperty: " + modifiedJson);

		Gson gson = new Gson();
		ObjectProperty objectProperty = gson.fromJson(json, ObjectProperty.class);
		ObjectProperty modifiedObjectProperty = gson.fromJson(modifiedJson, ObjectProperty.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();

		querStr.append("DELETE DATA { ");
		//querStr.append(datatypeProperty.getId() + " rdf:type owl:DataTypeProperty .");
		querStr.append("<" + objectProperty.getId() + "> rdfs:label \"" + objectProperty.getLabel() + "\" . ");
		querStr.append("<" + objectProperty.getId() + "> rdfs:range <" + objectProperty.getRange() + "> . ");
		querStr.append(" }");
		querStr1.append("INSERT DATA { ");
		//querStr1.append(datatypeProperty.getId() + " rdf:type owl:DataTypeProperty .");
		querStr1.append("<" + objectProperty.getId() + "> rdfs:label \"" + modifiedObjectProperty.getLabel() + "\" . ");
		querStr1.append("<" + objectProperty.getId() + "> rdfs:range <" + modifiedObjectProperty.getRange() + "> . ");
		querStr1.append(" }");

		//Model modelTpl = ModelFactory.createDefaultModel();
		
		ontology.insertQuery(querStr);
		ontology.insertQuery(querStr1);

		return Response.status(Status.OK).entity("{}").build();

	}

	@POST
	@Path("/deleteDatatypeProperty")
	public Response deleteDatatypeProperty(String json) {

		System.out.println("/Element received: " + json);

		Gson gson = new Gson();
		DatatypeProperty property = gson.fromJson(json, DatatypeProperty.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();

		/**
		 * Delete a class,
		 * all its predicates and values
		 * and all relations referencing this predicate
		 *
		 * DELETE
		 * WHERE {bpmn:NewSubprocess ?predicate  ?object .}
		 */

		querStr.append("DELETE "); //Does not work with DELETE DATA
		querStr.append("WHERE { ");
		querStr.append("<" + property.getId() + "> ?predicate ?object . ");
		querStr.append("?subject <" + property.getId() + "> ?data . ");
		querStr.append("}");

		
		ontology.insertQuery(querStr);
		return Response.status(Status.OK).entity("{}").build();
	}

	@POST
	@Path("/deleteObjectProperty")
	public Response deleteObjectProperty(String json) {

		System.out.println("/Element received: " + json);

		Gson gson = new Gson();
		ObjectProperty property = gson.fromJson(json, ObjectProperty.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();

		/**
		 * Delete a class and all its predicates and values
		 * DELETE
		 * WHERE {bpmn:NewSubprocess ?predicate  ?object .}
		 */

		querStr.append("DELETE ");
		querStr.append("WHERE { <" + property.getId() + "> ?predicate ?object . } ");
		
		ontology.insertQuery(querStr);
		return Response.status(Status.OK).entity("{}").build();
	}


	@POST
	@Path("/createDomainElement")
	public Response insertDomainElement(String json) {

		System.out.println("/element received: " + json);

		Gson gson = new Gson();
		DomainElement pElement = gson.fromJson(json, DomainElement.class);
		//pElement.setClassType("http://fhnw.ch/modelingEnvironment/LanguageOntology#PaletteElement");

		ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();
		querStr1.append("INSERT DATA {");
		querStr1.append("do:" + pElement.getId() + " rdf:type rdfs:Class . ");
		if (pElement.isRoot() == false)
			querStr1.append("do:" + pElement.getId() + " rdfs:subClassOf <" + pElement.getParentElement() + "> . ");
		else
			querStr1.append("do:" + pElement.getId() + " rdfs:subClassOf do:DomainOntologyConcept . ");
		querStr1.append("do:" + pElement.getId() + " rdfs:label \"" + pElement.getLabel() + "\" ");
		querStr1.append("}");
		//querStr1.append(" WHERE { }");

		System.out.println("Create subclass in Domain Ontology");
		System.out.println(querStr1.toString());
		
		ontology.insertQuery(querStr1);


		return Response.status(Status.OK).entity("{}").build();

	}

	@POST
	@Path("/createDatatypeProperty")
	public Response insertDatatypeProperty(String json) {

		System.out.println("/element received: " + json);

		Gson gson = new Gson();
		DatatypeProperty datatypeProperty = gson.fromJson(json, DatatypeProperty.class);
		//pElement.setClassType("http://fhnw.ch/modelingEnvironment/LanguageOntology#PaletteElement");

		if (datatypeProperty.getDomainName() != null) {
			String domainName = datatypeProperty.getDomainName();

			if (!domainName.contains("#")) {
				String[] domainArr = domainName.split(":");
				domainName = GlobalVariables.getNamespaceMap().get(domainArr[0]) + "#" + domainArr[1];
				System.out.println("Domain range to insert :" + domainName);
			}


			ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();
			querStr1.append("INSERT DATA {");
			System.out.println("    Property ID: " + datatypeProperty.getId());
			querStr1.append("lo:" + datatypeProperty.getId() + " rdf:type owl:DatatypeProperty . ");
			System.out.println("    Language Class: " + datatypeProperty.getDomainName());
			querStr1.append("lo:" + datatypeProperty.getId() + " rdfs:domain " + "<" + domainName + "> . ");
			System.out.println("    Property Label: " + datatypeProperty.getLabel());
			querStr1.append("lo:" + datatypeProperty.getId() + " rdfs:label \"" + datatypeProperty.getLabel() + "\" . ");
			System.out.println("    Property Range: " + datatypeProperty.getRange());
			querStr1.append("lo:" + datatypeProperty.getId() + " rdfs:range " + datatypeProperty.getRange() + " . ");
			System.out.println("    Availability to model: " + datatypeProperty.isAvailableToModel());
			querStr1.append("lo:" + datatypeProperty.getId() + " " + MODEL.getPrefix() + ":propertyIsShownInModel " + datatypeProperty.isAvailableToModel() + " . ");
			querStr1.append("}");
			//querStr1.append(" WHERE { }");

			System.out.println("Create Datatype property");
			System.out.println(querStr1.toString());
			
			ontology.insertQuery(querStr1);
		}


		return Response.status(Status.OK).entity("{}").build();

	}

	@POST
	@Path("/createBridgingConnector")
	public Response insertBCObjectProperty(String json) {

		System.out.println("/element received: " + json);

		Gson gson = new Gson();
		ObjectProperty objectProperty = gson.fromJson(json, ObjectProperty.class);
		//pElement.setClassType("http://fhnw.ch/modelingEnvironment/LanguageOntology#PaletteElement");

		if (objectProperty.getDomainName() != null) {
			String domainName = objectProperty.getDomainName();

			if (!domainName.contains("#")) {
				String[] domainArr = domainName.split(":");
				domainName = GlobalVariables.getNamespaceMap().get(domainArr[0]) + "#" + domainArr[1];
				System.out.println("Domain range to insert :" + domainName);
			}


			ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();
			querStr1.append("INSERT DATA {");
			System.out.println("    Property ID: " + objectProperty.getId());
			querStr1.append("lo:" + objectProperty.getId() + " rdf:type owl:ObjectProperty .");
			querStr1.append("lo:" + objectProperty.getId() + " rdfs:subPropertyOf lo:elementHasBridgingConcept . ");
			System.out.println("    Language Class: " + objectProperty.getDomainName());
			querStr1.append("lo:" + objectProperty.getId() + " rdfs:domain " + "<" + domainName + "> . ");
			System.out.println("    Property Label: " + objectProperty.getLabel());
			querStr1.append("lo:" + objectProperty.getId() + " rdfs:label \"" + objectProperty.getLabel() + "\" . ");
			System.out.println("    Property Range: " + objectProperty.getRange());
			querStr1.append("lo:" + objectProperty.getId() + " rdfs:range <" + objectProperty.getRange() + "> ");
			querStr1.append("}");
			//querStr1.append(" WHERE { }");

			System.out.println("Create Object property");
			System.out.println(querStr1.toString());
			
			ontology.insertQuery(querStr1);
		}


		return Response.status(Status.OK).entity("{}").build();

	}

	@POST
	@Path("/createSemanticMapping")
	public Response insertSMObjectProperty(String json) {

		System.out.println("/element received: " + json);

		Gson gson = new Gson();
		ObjectProperty objectProperty = gson.fromJson(json, ObjectProperty.class);
		//pElement.setClassType("http://fhnw.ch/modelingEnvironment/LanguageOntology#PaletteElement");

		if (objectProperty.getDomainName() != null) {
			String domainName = objectProperty.getDomainName();

			if (!domainName.contains("#")) {
				String[] domainArr = domainName.split(":");
				domainName = GlobalVariables.getNamespaceMap().get(domainArr[0]) + "#" + domainArr[1];
				System.out.println("Domain range to insert :" + domainName);
			}


			ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();
			querStr1.append("INSERT DATA {");
			System.out.println("    Property ID: " + objectProperty.getId());
			querStr1.append("lo:" + objectProperty.getId() + " rdf:type owl:ObjectProperty .");
			querStr1.append("lo:" + objectProperty.getId() + " rdfs:subPropertyOf lo:elementIsMappedWithDOConcept . ");
			System.out.println("    Language Class: " + objectProperty.getDomainName());
			querStr1.append("lo:" + objectProperty.getId() + " rdfs:domain " + "<" + domainName + "> . ");
			System.out.println("    Property Label: " + objectProperty.getLabel());
			querStr1.append("lo:" + objectProperty.getId() + " rdfs:label \"" + objectProperty.getLabel() + "\" . ");
			System.out.println("    Property Range: " + objectProperty.getRange());
			querStr1.append("lo:" + objectProperty.getId() + " rdfs:range <" + objectProperty.getRange() + "> ");
			querStr1.append("}");
			//querStr1.append(" WHERE { }");

			System.out.println("Create Object property");
			System.out.println(querStr1.toString());
			
			ontology.insertQuery(querStr1);
		}


		return Response.status(Status.OK).entity("{}").build();

	}

	@POST
	@Path("/createShaclConstraint")
	public Response insertShaclConstraints(String json) {
		System.out.println("/element received: " + json);

		Gson gson = new Gson();
		ShaclConstraint shaclConstraint = gson.fromJson(json, ShaclConstraint.class);

		if (shaclConstraint.getTargetClass() != null) {
			String targetClass = shaclConstraint.getTargetClass();

			if (!targetClass.contains("#")) {
				String[] targetClassArr = targetClass.split(":");
				targetClass = GlobalVariables.getNamespaceMap().get(targetClassArr[0]) + "#" + targetClassArr[1];
				System.out.println("Domain range to insert :" + targetClass);
			}

			String constraintName = shaclConstraint.getName();
			System.out.println("Constraint name to insert: " + constraintName);

			ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
			queryStr.append("INSERT DATA {\n");
			System.out.println("    Property ID: " + shaclConstraint.getId());
			queryStr.append("sh:" + shaclConstraint.getId() + " rdf:type sh:PropertyShape . ");
			queryStr.append("sh:" + shaclConstraint.getId() + " sh:targetClass <" + targetClass + "> . ");
			queryStr.append("sh:" + shaclConstraint.getId() + " sh:name \"" + shaclConstraint.getName() + "\" . ");
			queryStr.append("sh:" + shaclConstraint.getId() + " rdfs:domain <" + targetClass + "> . ");
			if (shaclConstraint.getDescription() != null && !shaclConstraint.getDescription().equals("null"))
				queryStr.append("sh:" + shaclConstraint.getId() + " sh:description \"" + shaclConstraint.getDescription() + "\" . ");
			queryStr.append("sh:" + shaclConstraint.getId() + " sh:path <" + shaclConstraint.getPath() + "> . ");
			if(shaclConstraint.getDatatype() != null)
				queryStr.append("sh:" + shaclConstraint.getId() + " sh:datatype " + shaclConstraint.getDatatype() + " . ");
			if(shaclConstraint.getPattern() != null) {
				String escapedPattern = shaclConstraint.getPattern().replace("\\", "\\\\");
				queryStr.append("sh:" + shaclConstraint.getId() + " sh:pattern \"" + escapedPattern + "\" . ");
			}
			if(shaclConstraint.getMinCount() != null)
				queryStr.append("sh:" + shaclConstraint.getId() + " sh:minCount " + shaclConstraint.getMinCount() + " . ");
			if(shaclConstraint.getMaxCount() != null)
				queryStr.append("sh:" + shaclConstraint.getId() + " sh:maxCount " + shaclConstraint.getMaxCount() + " . ");
			queryStr.append("}");

			System.out.println("Create Shacl Constraint");
			System.out.println(queryStr);
			
			ontology.insertQuery(queryStr);
		}

		return Response.status(Status.OK).entity("{}").build();
	}

	/**
	 * For Testing SHACL
	 * Prints the SHACL validation report in Turtle to the console
	 * @return Response with the validation report in JSON
	 */
	@GET
	@Path("/validateShaclTest" )
	public Response validateShaclTest() {
		org.apache.jena.rdf.model.Model constraintsModel = ModelFactory.createDefaultModel();
		org.apache.jena.rdf.model.Model dataModel = ModelFactory.createDefaultModel();
		String SHAPES = "C:/Users/Kiril/eclipse-workspace/AOAME/OntologyBasedModellingEnvironment-WebApp-scripts/aoame/OntologyBasedModellingEnvironment-WebService/AOAME.ttl";
		String DATA = "C:/Users/Kiril/eclipse-workspace/AOAME/OntologyBasedModellingEnvironment-WebApp-scripts/aoame/OntologyBasedModellingEnvironment-WebService/SHACL.ttl";

		constraintsModel.read(SHAPES);
		dataModel.read(DATA);

		//Graph shapesGraph = RDFDataMgr.loadGraph(SHAPES);
		//Graph dataGraph = RDFDataMgr.loadGraph(DATA);

		// Load the data and constraints into their Models
		try {
			dataModel.read(new FileInputStream(SHAPES), "", "TURTLE");
			constraintsModel.read(new FileInputStream(DATA), "", "TURTLE");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		// Carry out the validation
		ValidationReport report = ShaclValidator.get().validate(constraintsModel.getGraph(), dataModel.getGraph());
		// Print the report
		RDFDataMgr.write(System.out, report.getModel(), Lang.TURTLE);
		// Prepare a list that will hold the report entries as Maps
		List<Map<String, Object>> entriesList = new ArrayList<>();
		// Go through each report entry
		for (ReportEntry entry : report.getEntries()) {
			// Prepare a map that will hold the entry's data
			Map<String, Object> entryMap = new HashMap<>();
			// Add the data to the map
			entryMap.put("focusNode", entry.focusNode().toString());
			entryMap.put("resultPath", entry.resultPath().toString());
			entryMap.put("resultSeverity", entry.severity().toString());
			entryMap.put("resultMessage", entry.message());
			// Add the map to the list
			entriesList.add(entryMap);
		}
		// Convert validation report to JSON
		String json = gson.toJson(entriesList);

		return Response.status(Status.OK).entity(json).build();
	}

	@GET
	@Path("/validateShacl/{modelId}" )
	public Response validateShacl(@PathParam("modelId") String modelId) throws NoResultsException {
		// Load data and SHACL constraints into Jena model

		org.apache.jena.rdf.model.Model model = ModelFactory.createDefaultModel();
		org.apache.jena.rdf.model.Model propertiesModel = ModelFactory.createDefaultModel();

		ArrayList<ShaclConstraint> shaclConstraints;

		try {
			shaclConstraints = queryAllShaclConstraints();
		} catch (NoResultsException e) {
			throw new RuntimeException(e);
		}
		if (shaclConstraints.isEmpty()) {
			return Response.ok("No constraints were found").build();
		}

		// Store all domain names
		List<String> domainNames = new ArrayList<String>();

		// Get all constraints
		for (ShaclConstraint constraint : shaclConstraints) {
			Resource resource = model.createResource(constraint.getId())
					.addProperty(RDF.type, SHACL.PropertyShape.getURI())
					.addProperty(model.createProperty(SHACL.path.getURI()), model.createProperty(constraint.getPath()));

			if (constraint.getDescription() != null) {
				resource.addProperty(model.createProperty(SHACL.description.getURI()), constraint.getDescription());
			}
			if (constraint.getName() != null) {
				resource.addProperty(model.createProperty(SHACL.name.getURI()), constraint.getName());
			}
			if (constraint.getTargetClass() != null) {
				resource.addProperty(model.createProperty(SHACL.targetClass.getURI()), constraint.getTargetClass());
			}
			if(constraint.getDatatype() != null) {
				resource.addProperty(model.createProperty(SHACL.datatype.getURI()), model.createResource(constraint.getDatatype()));
			}
			if(constraint.getPattern() != null) {
				resource.addProperty(model.createProperty(SHACL.pattern.getURI()), model.createTypedLiteral(constraint.getPattern()));
			}
			if(constraint.getMinCount() != null) {
				String minCountStr = constraint.getMinCount().split("\\^\\^")[0];
				resource.addProperty(model.createProperty(SHACL.minCount.getURI()), model.createTypedLiteral(Integer.parseInt(minCountStr)));
			}
			if(constraint.getMaxCount() != null) {
				String maxCountStr = constraint.getMaxCount().split("\\^\\^")[0];
				resource.addProperty(model.createProperty(SHACL.maxCount.getURI()), model.createTypedLiteral(Integer.parseInt(maxCountStr)));
			}

			//Add each domain name only once
			if (!domainNames.contains(constraint.getDomainName())){
				domainNames.add(constraint.getDomainName());
			}
		}
		model.write(System.out, "TURTLE");
		// make sure it returns all properties from all domains
		List<ObjectProperty> objectProperties = new ArrayList<>();

		try {
			objectProperties = queryElementInstances(modelId);
		} catch (NoResultsException e) {
			throw new RuntimeException(e);
		}

		for (ObjectProperty objectProperty : objectProperties) {
			Resource resource = propertiesModel.createResource(objectProperty.getId());

			Property property = propertiesModel.createProperty(objectProperty.getLabel());
			RDFNode value;
			if (objectProperty.getRange().contains("^^")){
				String range = objectProperty.getRange().split("\\^\\^")[0];
				String datatype = objectProperty.getRange().split("\\^\\^")[1];
				String fullDatatypeUri = NAMESPACE.XSD.getURI();
				value = propertiesModel.createTypedLiteral(range, datatype);
			} else {
				value = propertiesModel.createTypedLiteral(objectProperty.getRange());
			}
			resource.addProperty(property, value);
		}
		propertiesModel.write(System.out, "TURTLE");

		// Create a ShapesGraph from the model
		RDFDataMgr.write(System.out, model, Lang.TURTLE);
		RDFDataMgr.write(System.out, propertiesModel, Lang.TURTLE);

		// Perform SHACL validation
		ShaclValidator validator = ShaclValidator.get();
		ValidationReport report = validator.validate(model.getGraph(), propertiesModel.getGraph());

		// Print validation report
		ShLib.printReport(report);
		System.out.println("Valid: " + report.conforms());

		// Extract report Entries and return as JSON
		List<ReportEntry> reportEntries = new ArrayList<>(report.getEntries());

		JSONArray jsonArray = new JSONArray();

		for(ReportEntry reportEntry : reportEntries) {
			// Extract information from the report entry
			Node focusNode = reportEntry.focusNode();
			org.apache.jena.sparql.path.Path resultPath = reportEntry.resultPath();
			Severity severity = reportEntry.severity();
			String message = reportEntry.message();

			JSONObject jsonEntry = new JSONObject();
			jsonEntry.put("FocusNode", focusNode.toString());
			jsonEntry.put("Path", resultPath.toString().split("#")[1].split(">")[0]);
			jsonEntry.put("Severity", severity.level().toString().split("#")[1].split(">")[0]);
			jsonEntry.put("Message", message);

			jsonArray.put(jsonEntry);
		}

		// Convert validation report to JSON
		String json = jsonArray.toString();

		return Response.status(Status.OK).entity(json).build();
	}

	private ArrayList<ObjectProperty> queryElementInstances(String id) throws NoResultsException {
		String modelId = String.format("%s:%s", MODEL.getPrefix(), id);

		String command = String.format(
				"SELECT ?diag\n" +
						"WHERE {\n" +
						"\t%1$s %2$s:modelHasShape ?diag .\n" +
						"}",
				modelId,
				MODEL.getPrefix()
		);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		
		ResultSet resultSet = ontology.query(query).execSelect();

		List<String> shapeIds = new ArrayList<>();

		while (resultSet.hasNext()) {
			QuerySolution solution = resultSet.next();
			shapeIds.add(MODEL.getPrefix() + ':' + extractIdFrom(solution, "?diag"));
		}

		ArrayList<ObjectProperty> result = new ArrayList<>();

		shapeIds.forEach(shapeId -> {

			String command2 = String.format(
					"SELECT ?diag\n" +
							"WHERE {\n" +
							"\t%1$s %2$s:shapeVisualisesConceptualElement ?diag .\n" +
							"}",
					shapeId,
					MODEL.getPrefix()
			);
			System.out.println(command2);

			ParameterizedSparqlString query2 = new ParameterizedSparqlString(command2);
			
			ResultSet resultSet2 = ontology.query(query2).execSelect();

			List<String> elementsIds = new ArrayList<>();

			if (resultSet2.hasNext()) {
				while (resultSet2.hasNext()) {
					QuerySolution solution = resultSet2.next();
					elementsIds.add(MODEL.getPrefix() + ':'+extractIdFrom(solution, "?diag"));
				}
			}
			ParameterizedSparqlString query3 = new ParameterizedSparqlString();

			for (String elementId: elementsIds) {
				query3.append("SELECT ?property ?value " +
						"WHERE { " +
						elementId + " ?property ?value . " +
						"} ");
				
				QueryExecution qexec = ontology.query(query3);
				ResultSet results = qexec.execSelect();
				if (results.hasNext()) {
					while (results.hasNext()) {
						ObjectProperty objectProperty = new ObjectProperty();
						QuerySolution soln = results.next();
						//System.out.println(nm + " " + GlobalVariables.getNamespaceMap().get(nm));
						objectProperty.setId(elementId);
						objectProperty.setLabel(soln.get("?property").toString());
						objectProperty.setRange(soln.get("?value").toString());

						result.add(objectProperty);
					}
				} else try {
					throw new NoResultsException("No results found");
				} catch (NoResultsException e) {
					throw new RuntimeException(e);
				}
			}
		});

		return result;
	}

	private ArrayList<Answer> queryShaclConstraints() throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<Answer> result = new ArrayList<Answer>();

		queryStr.append("SELECT ?id ?label WHERE { " +
						"?id ?label  . \n" +
						"FILTER (str(?object) = \"bpmn\") }");

		
		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();

		if (results.hasNext()) {
			while (results.hasNext()) {
				Answer ans = new Answer();

				QuerySolution soln = results.next();
				String nm = soln.get("?id").toString().split("#")[0];
				//System.out.println(nm + " " + GlobalVariables.getNamespaceMap().get(nm));
				ans.setId(soln.get("?id").toString());
				ans.setLabel(GlobalVariables.getNamespaceMap().get(nm) + ":" + soln.get("?label").toString());

				result.add(ans);
			}
		}
		qexec.close();

		return result;
	}

	@GET
	@Path("/getDomainOntologyClasses")
	public Response getDomainOntologyElements() {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested domain ontology elements");
		System.out.println("####################<end>####################");
		ArrayList<Answer> all_do_elements = new ArrayList<Answer>();

		try {
			all_do_elements = queryDOElements();

			if (debug_properties) {
				for (int index = 0; index < all_do_elements.size(); index++) {
					System.out.println("Element " + index + ": " + all_do_elements.get(index).getId());
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(all_do_elements);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " + json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}

	private ArrayList<Answer> queryDOElements() throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<Answer> result = new ArrayList<Answer>();

		queryStr.append("SELECT DISTINCT ?id ?label WHERE {");
		queryStr.append("?id a ?type .");
		queryStr.append("?id rdfs:label ?label .");
		queryStr.append("FILTER(?type != 'rdf:class') .");
		//queryStr.append("FILTER(STRSTARTS(STR(?id),STR(\"http://fhnw.ch/modelingEnvironment/DomainOntology#\"))) .");
		queryStr.append("?id rdfs:subClassOf* do:DomainOntologyConcept .");
		queryStr.append("FILTER(?id != do:DomainOntologyConcept) .");
		queryStr.append("}");
		queryStr.append("ORDER BY ?label");

		
		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();

		if (results.hasNext()) {
			while (results.hasNext()) {
				Answer ans = new Answer();

				QuerySolution soln = results.next();
				String nm = soln.get("?id").toString().split("#")[0];
				//System.out.println(nm + " " + GlobalVariables.getNamespaceMap().get(nm));
				ans.setId(soln.get("?id").toString());
				ans.setLabel(GlobalVariables.getNamespaceMap().get(nm) + ":" + soln.get("?label").toString());

				result.add(ans);
			}
		}
		qexec.close();

		return result;
	}

	@GET
	@Path("/getModelingLanguageOntologyElements")
	public Response getModelingLanguageOntologyElements() {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested modeling language ontology elements");
		System.out.println("####################<end>####################");
		ArrayList<Answer> all_ml_elements = new ArrayList<Answer>();

		try {
			all_ml_elements = queryMLElements();

			if (debug_properties) {
				for (int index = 0; index < all_ml_elements.size(); index++) {
					System.out.println("Element " + index + ": " + all_ml_elements.get(index).getId());
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(all_ml_elements);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " + json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}

	private ArrayList<Answer> queryMLElements() throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<Answer> result = new ArrayList<Answer>();

		queryStr.append("SELECT DISTINCT ?id ?label WHERE {");
		queryStr.append("?id a ?type .");
		queryStr.append("?id rdfs:label ?label .");
		queryStr.append("?id rdfs:subClassOf* lo:ModelingElement .");
		queryStr.append("FILTER(?id != lo:ModelingElement) .");
		queryStr.append("}");
		queryStr.append("ORDER BY ?id");

		
		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();

		if (results.hasNext()) {
			while (results.hasNext()) {
				Answer ans = new Answer();

				QuerySolution soln = results.next();
				ans.setId(soln.get("?id").toString());
				String namespace = soln.get("?id").toString().split("#")[0];
				//System.out.println("namespace :"+namespace);
				ans.setLabel(GlobalVariables.getNamespaceMap().get(namespace) + ":" + soln.get("?label").toString());

				result.add(ans);
			}
		}
		qexec.close();

		return result;
	}

	@GET
	@Path("/getDatatypeProperties/{domainName}")
	public Response getDatatypeProperties(@PathParam("domainName") String domainName) {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested datatype properties for " + domainName);
		System.out.println("####################<end>####################");
		ArrayList<DatatypeProperty> datatype_properties = new ArrayList<DatatypeProperty>();

		try {
			if (domainName != null) {
				String[] domainNameArr = domainName.split(":");
				domainName = GlobalVariables.getNamespaceMap().get(domainNameArr[0].toLowerCase()) + "#" + domainNameArr[1];
				System.out.println("domain range for query is : " + domainName);
				datatype_properties = queryAllDatatypeProperties(domainName);

				if (debug_properties) {
					for (int index = 0; index < datatype_properties.size(); index++) {
						System.out.println("Domain " + index + ": ");
					}
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(datatype_properties);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " + json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}

	private ArrayList<DatatypeProperty> queryAllDatatypeProperties(String domainName) throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<DatatypeProperty> result = new ArrayList<DatatypeProperty>();

		queryStr.append("SELECT DISTINCT ?id ?domain ?range ?label ?isAvailableToModel WHERE {");
		queryStr.append("?id a ?type . FILTER(?type IN (owl:DatatypeProperty)) . ");
		queryStr.append("?id rdfs:domain ?domain . ");
		queryStr.append("FILTER(?domain IN (<" + domainName + ">)) . ");
		queryStr.append("?id rdfs:label ?label . ");
		queryStr.append("?id rdfs:range ?range . ");
		queryStr.append("OPTIONAL {?id " + MODEL.getPrefix() + ":propertyIsShownInModel ?isAvailableToModel} ");
		//queryStr.append("OPTIONAL {?domain rdf:type owl:DataTypeProperty} ");

		queryStr.append("} ");
		queryStr.append("ORDER BY ?label");

		
		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();

		if (results.hasNext()) {
			while (results.hasNext()) {
				DatatypeProperty datatypeProperty = new DatatypeProperty();

				QuerySolution soln = results.next();
				datatypeProperty.setId(soln.get("?id").toString());
				datatypeProperty.setLabel(soln.get("?label").toString());
				datatypeProperty.setDomainName(domainName);
				datatypeProperty.setRange(extractNamespaceAndIdFrom(soln, "?range"));
				RDFNode rdfNode = soln.get("?isAvailableToModel");
				if (rdfNode != null) datatypeProperty.setAvailableToModel(((LiteralImpl) rdfNode).getBoolean());

				result.add(datatypeProperty);
			}
		}
		qexec.close();
		return result;
	}

	@GET
	@Path("/getBridgeConnectors/{domainName}")
	public Response getBCObjectProperties(@PathParam("domainName") String domainName) {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested datatype properties for " + domainName);
		System.out.println("####################<end>####################");
		ArrayList<ObjectProperty> object_properties = new ArrayList<ObjectProperty>();

		try {
			if (domainName != null) {
				String[] domainNameArr = domainName.split(":");
				domainName = GlobalVariables.getNamespaceMap().get(domainNameArr[0].toLowerCase()) + "#" + domainNameArr[1];
				System.out.println("domain range for query is : " + domainName);
				object_properties = queryAllBCObjectProperties(domainName);

				if (debug_properties) {
					for (int index = 0; index < object_properties.size(); index++) {
						System.out.println("Domain " + index + ": ");
					}
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(object_properties);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " + json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}

	private ArrayList<ObjectProperty> queryAllBCObjectProperties(String domainName) throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<ObjectProperty> result = new ArrayList<ObjectProperty>();

		queryStr.append("SELECT DISTINCT ?id ?domain ?range ?label WHERE {");
		queryStr.append("?id rdfs:subPropertyOf ?subProperty . FILTER(?subProperty IN (lo:elementHasBridgingConcept)) . "); //lo:elementIsMappedWithDOConcept, lo:hasBridgingConcept
		queryStr.append("?id rdfs:domain ?domain . ");
		queryStr.append("FILTER(?domain IN (<" + domainName + ">)) . ");
		queryStr.append("?id rdfs:label ?label . ");
		queryStr.append("?id rdfs:range ?range . ");

		queryStr.append("} ");
		queryStr.append("ORDER BY ?label");

		
		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();

		if (results.hasNext()) {
			while (results.hasNext()) {
				ObjectProperty objectProperty = new ObjectProperty();

				QuerySolution soln = results.next();
				objectProperty.setId(soln.get("?id").toString());
				objectProperty.setLabel(soln.get("?label").toString());
				objectProperty.setDomainName(domainName);
				objectProperty.setRange(soln.get("?range").toString());

				result.add(objectProperty);
			}
		}
		qexec.close();
		return result;
	}

	@GET
	@Path("/getSemanticMappings/{domainName}")
	public Response getSMObjectProperties(@PathParam("domainName") String domainName) {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested datatype properties for " + domainName);
		System.out.println("####################<end>####################");
		ArrayList<ObjectProperty> object_properties = new ArrayList<ObjectProperty>();

		try {
			if (domainName != null) {
				String[] domainNameArr = domainName.split(":");
				domainName = GlobalVariables.getNamespaceMap().get(domainNameArr[0].toLowerCase()) + "#" + domainNameArr[1];
				System.out.println("domain range for query is : " + domainName);
				object_properties = queryAllObjectProperties(domainName);

				if (debug_properties) {
					for (int index = 0; index < object_properties.size(); index++) {
						System.out.println("Domain " + index + ": ");
					}
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(object_properties);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " + json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}

	private ArrayList<ObjectProperty> queryAllObjectProperties(String domainName) throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<ObjectProperty> result = new ArrayList<ObjectProperty>();

		queryStr.append("SELECT DISTINCT ?id ?domain ?range ?label WHERE {");
		queryStr.append("?id rdfs:subPropertyOf ?subProperty . FILTER(?subProperty IN (lo:elementIsMappedWithDOConcept)) . "); //lo:elementIsMappedWithDOConcept, lo:hasBridgingConcept
		queryStr.append("?id rdfs:domain ?domain . ");
		queryStr.append("FILTER(?domain IN (<" + domainName + ">)) . ");
		queryStr.append("?id rdfs:label ?label . ");
		queryStr.append("?id rdfs:range ?range . ");

		queryStr.append("} ");
		queryStr.append("ORDER BY ?label");

		
		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();

		if (results.hasNext()) {
			while (results.hasNext()) {
				ObjectProperty objectProperty = new ObjectProperty();

				QuerySolution soln = results.next();
				objectProperty.setId(soln.get("?id").toString());
				objectProperty.setLabel(soln.get("?label").toString());
				objectProperty.setDomainName(domainName);
				objectProperty.setRange(soln.get("?range").toString());

				result.add(objectProperty);
			}
		}
		qexec.close();
		return result;
	}

	@GET
	@Path("/getAllProperties/{domainName}")
	public Response getAllObjectProperties(@PathParam("domainName") String domainName) {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested datatype properties for " + domainName);
		System.out.println("####################<end>####################");
		ArrayList<ObjectProperty> object_properties = new ArrayList<ObjectProperty>();

		try {
			if (domainName != null) {
				String[] domainNameArr = domainName.split(":");
				domainName = GlobalVariables.getNamespaceMap().get(domainNameArr[0].toLowerCase()) + "#" + domainNameArr[1];
				System.out.println("domain range for query is : " + domainName);
				object_properties = queryAllProperties(domainName);

				if (debug_properties) {
					for (int index = 0; index < object_properties.size(); index++) {
						System.out.println("Domain " + index + ": ");
					}
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}

		String json = gson.toJson(object_properties);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " + json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}

	private ArrayList<ObjectProperty> queryAllProperties(String domainName) throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<ObjectProperty> result = new ArrayList<>();

		queryStr.append("SELECT DISTINCT ?id ?domain ?range ?label WHERE { " +
				"{ ?id a owl:DatatypeProperty . " +
				"?id rdfs:domain ?domain . " +
				"FILTER(?domain = <" + domainName + ">) . " +
				"?id rdfs:label ?label . " +
				"?id rdfs:range ?range . }" +
				"UNION " +
				"{ ?id rdfs:subPropertyOf lo:elementIsMappedWithDOConcept . " +
				"?id rdfs:domain ?domain . " +
				"FILTER(?domain = <" + domainName + ">) . " +
				"?id rdfs:label ?label . " +
				"?id rdfs:range ?range . }" +
				"UNION " +
				"{ ?id rdfs:subPropertyOf lo:elementHasBridgingConcept . " +
				"?id rdfs:domain ?domain . " +
				"FILTER(?domain = <" + domainName + ">) . " +
				"?id rdfs:label ?label . " +
				"?id rdfs:range ?range . }" +
				"} " +
				"ORDER BY ?label");

		
		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();

		if (results.hasNext()) {
			while (results.hasNext()) {
				ObjectProperty objectProperty = new ObjectProperty();

				QuerySolution soln = results.next();
				objectProperty.setId(soln.get("?id").toString());
				objectProperty.setLabel(soln.get("?label").toString());
				objectProperty.setDomainName(domainName);
				objectProperty.setRange(soln.get("?range").toString());

				result.add(objectProperty);
			}
		}
		qexec.close();
		return result;
	}

	@GET
	@Path("/getShaclConstraints/{targetClass}")
	public Response getShaclConstraints(@PathParam("targetClass") String targetClass) {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested shacl constraints for " + targetClass);
		System.out.println("####################<end>####################");
		ArrayList<ShaclConstraint> shacl_constraints = new ArrayList<>();

		try {
			if (targetClass != null) {
				String[] targetClassArr = targetClass.split(":");
				targetClass = GlobalVariables.getNamespaceMap().get(targetClassArr[0].toLowerCase()) + "#" + targetClassArr[1];
				System.out.println("domain range for query is : " + targetClass);
				shacl_constraints = queryShaclConstraints(targetClass);

				if (debug_properties) {
					for (int index = 0; index < shacl_constraints.size(); index++) {
						System.out.println("Domain " + index + ": ");
					}
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}

		String json = gson.toJson(shacl_constraints);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " + json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}

	// Queries all constraints for a specific class
	// TargetClass is the domainName in SHACL
	private ArrayList<ShaclConstraint> queryShaclConstraints(String targetClass) throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<ShaclConstraint> result = new ArrayList<>();

		queryStr.append("SELECT DISTINCT ?id ?name ?domainName ?description ?targetClass ?path ?datatype ?pattern ?minCount ?maxCount WHERE {");
		queryStr.append("?id a sh:PropertyShape . ");
		queryStr.append("?id sh:name ?name . ");
		queryStr.append("FILTER(?targetClass IN (<" + targetClass + ">)) . ");
		queryStr.append("OPTIONAL { ?id sh:description ?description . }");
		queryStr.append("OPTIONAL { ?id sh:targetClass ?targetClass . }");
		queryStr.append("OPTIONAL { ?id sh:path ?path . }");
		queryStr.append("OPTIONAL { ?id sh:datatype ?datatype . }");
		queryStr.append("OPTIONAL { ?id sh:pattern ?pattern . }");
		queryStr.append("OPTIONAL { ?id sh:minCount ?minCount . }");
		queryStr.append("OPTIONAL { ?id sh:maxCount ?maxCount . }");
		queryStr.append("}");

		
		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();

		if (results.hasNext()) {
			while (results.hasNext()) {
				ShaclConstraint shaclConstraint = new ShaclConstraint();

				QuerySolution soln = results.next();
				shaclConstraint.setId(soln.get("?id").toString());
				shaclConstraint.setName(soln.get("?name").toString());
				if(soln.contains("?description"))
					shaclConstraint.setDescription(soln.get("?description").toString());
				if(soln.contains("?targetClass"))
					shaclConstraint.setTargetClass(soln.get("?targetClass").toString());
				if(soln.contains("?path"))
					shaclConstraint.setPath(soln.get("?path").toString());
				if(soln.contains("?datatype"))
					shaclConstraint.setDatatype(soln.get("?datatype").toString());
				if(soln.contains("?pattern"))
					shaclConstraint.setPattern(soln.get("?pattern").toString());
				if(soln.contains("?minCount"))
					shaclConstraint.setMinCount(soln.get("?minCount").toString());
				if(soln.contains("?maxCount"))
					shaclConstraint.setMaxCount(soln.get("?maxCount").toString());

				result.add(shaclConstraint);
			}
		}
		qexec.close();
		return result;
	}

	// Extracts all constraints from the ontology
	private ArrayList<ShaclConstraint> queryAllShaclConstraints() throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<ShaclConstraint> result = new ArrayList<>();

		queryStr.append("SELECT DISTINCT ?id ?name ?domainName ?description ?targetClass ?path ?datatype ?pattern ?minCount ?maxCount WHERE {");
		queryStr.append("?id a sh:PropertyShape . ");
		queryStr.append("?id sh:name ?name . ");
		queryStr.append("?id rdfs:domain ?domainName . ");
		queryStr.append("OPTIONAL { ?id sh:description ?description . }");
		queryStr.append("OPTIONAL { ?id sh:targetClass ?targetClass . }");
		queryStr.append("OPTIONAL { ?id sh:path ?path . }");
		queryStr.append("OPTIONAL { ?id sh:datatype ?datatype . }");
		queryStr.append("OPTIONAL { ?id sh:pattern ?pattern . }");
		queryStr.append("OPTIONAL { ?id sh:minCount ?minCount . }");
		queryStr.append("OPTIONAL { ?id sh:maxCount ?maxCount . }");
		queryStr.append("}");

		
		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();

		if (results.hasNext()) {
			while (results.hasNext()) {
				ShaclConstraint shaclConstraint = new ShaclConstraint();

				QuerySolution soln = results.next();
				shaclConstraint.setId(soln.get("?id").toString());
				shaclConstraint.setName(soln.get("?name").toString());
				if(soln.contains("?description"))
					shaclConstraint.setDescription(soln.get("?description").toString());
				if(soln.contains("?targetClass"))
					shaclConstraint.setTargetClass(soln.get("?targetClass").toString());
				if(soln.contains("?path"))
					shaclConstraint.setPath(soln.get("?path").toString());
				if(soln.contains("?datatype"))
					shaclConstraint.setDatatype(soln.get("?datatype").toString());
				if(soln.contains("?pattern"))
					shaclConstraint.setPattern(soln.get("?pattern").toString());
				if(soln.contains("?minCount"))
					shaclConstraint.setMinCount(soln.get("?minCount").toString());
				if(soln.contains("?maxCount"))
					shaclConstraint.setMaxCount(soln.get("?maxCount").toString());

				result.add(shaclConstraint);
			}
		}
		qexec.close();
		return result;
	}

	@GET
	@Path("/getAllNamespacePrefixes")
	public Response getAllNamespacePrefixes() {
		ArrayList<String> prefixList = new ArrayList<String>();
		for (NAMESPACE ns : NAMESPACE.values()) {
			prefixList.add(ns.getPrefix() + ":");
		}
		Collections.sort(prefixList);
		return Response.status(Status.OK).entity(gson.toJson(prefixList)).build();
	}

	@GET
	@Path("/ping123")
	public Response getTest() {
		return Response.ok().entity("Service online").build();
	}


	@GET
	@Path("/getNamespaceMap")
	public Response getNamespaceMap() {
		System.out.println("Returning namespace map: " + gson.toJson(GlobalVariables.getNamespaceMap()));
		return Response.status(Status.OK).entity(gson.toJson(GlobalVariables.getNamespaceMap())).build();
	}


	@GET
	@Path("/images")
	@Produces("image/png")
	public Response getCompanyLogo() throws IOException {
		String filePath = "images/Test_Agent.png";
		File file = new File(filePath);

		ResponseBuilder response = Response.ok((Object) file);

		response.header("Content-Disposition",
				"attachment; filename=Test_Agent.png");

		return response.build();
	}

	@GET
	@Path("getTTL")
	public Response getRequestREADENDPOINT() throws IOException {


		URL url = new URL(OntologyManager.getREADENDPOINT());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");

		con.setRequestProperty("Content-Type", "text/trig");
		String contentType = con.getHeaderField("Content-Type");


		int status = con.getResponseCode();

		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
			content.append(System.getProperty("line.separator"));
		}
		in.close();

		con.disconnect();

		String payload = gson.toJson(content);
		return Response.status(Status.OK).entity(payload).build();

	}

	@POST
	@Path("getTTLAdwithDistinction2")
	public Response getRequestREADENDPOINTAdvancedwithDistinction2(List<String> sPrefix) throws IOException {

		URL url = new URL(OntologyManager.getREADENDPOINT());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");

		con.setRequestProperty("Content-Type", "text/trig");
		String contentType = con.getHeaderField("Content-Type");


		int status = con.getResponseCode();


		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
			content.append(System.getProperty("line.separator"));
		}
		in.close();

		con.disconnect();

// GETPREFIXES FROM TTL
		String sRegex = "@prefix(.*?) \\.";

		Pattern pattern = Pattern.compile(sRegex);
		Matcher matcher = pattern.matcher(content);

		String sResult = "";
		// Check all occurrences
		while (matcher.find()) {
			//for local version
			//sResult = sResult + matcher.group()+"\r\n";
			//If local version the regex has to change
			if (OntologyManager.getTRIPLESTOREENDPOINT() == "http://localhost:3030/ModEnv") {
				sResult = sResult + matcher.group() + "\r\n";
			} else {
				//for deployed
				sResult = sResult + matcher.group() + "\n";

			}

		}
		for (String element : sPrefix) {

			String sPrefixForRegex = element;
			//this is for local version
			//String sRegex2 = "\\r\\n(?s)" + sPrefixForRegex + ":(.*?) \\.";
			//this is for deployed app
			String sRegex2 = "\\n(?s)" + sPrefixForRegex + ":(.*?) \\.";


			//if local version the regex change
			if (OntologyManager.getTRIPLESTOREENDPOINT() == "http://localhost:3030/ModEnv") {
				String sPrefixForRegex2 = sPrefixForRegex.replace("\r", "");
				sRegex2 = "\\r\\n(?s)" + sPrefixForRegex2 + ":(.*?) \\.";
			}
			Pattern pattern2 = Pattern.compile(sRegex2);
			Matcher matcher2 = pattern2.matcher(content);

			// Check all occurrences
			while (matcher2.find()) {
				sResult = sResult + matcher2.group();
			}
		}
		String sResultJson = gson.toJson(sResult);

		return Response.status(Status.OK).entity(sResultJson).build();

	}

	@GET
	@Path("getPrefixesFromFuseki")
	public Response getPrefixesFromFuseki() throws IOException {


		URL url = new URL(OntologyManager.getREADENDPOINT());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");

		con.setRequestProperty("Content-Type", "text/trig");
		String contentType = con.getHeaderField("Content-Type");


		int status = con.getResponseCode();
		//Finally, let's read the response of the request and place it in a content String:

		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
			content.append(System.getProperty("line.separator"));
		}
		in.close();
		//To close the connection, we can use the disconnect() method:

		con.disconnect();


		String sRegex = "\\r\\n(?s)([^@ ].*?):";

		Pattern pattern = Pattern.compile(sRegex);
		Matcher matcher = pattern.matcher(content);

		String sResult = "";
		// Check all occurrences
		while (matcher.find()) {

			if (!sResult.contains(matcher.group())) {

				sResult = sResult + matcher.group();

			}

		}

		sResult = sResult.replace("\r\n", "");

		sResult = sResult.replace(":", ",");
		String jsonPrefixes = gson.toJson(sResult);

		return Response.status(Status.OK).entity(jsonPrefixes).build();

	}

	@GET
	@Path("getPrefixesFromFuseki2")
	public Response getPrefixesFromFuseki2() throws IOException {

		URL url = new URL(OntologyManager.getREADENDPOINT());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");

		con.setRequestProperty("Content-Type", "text/trig");
		String contentType = con.getHeaderField("Content-Type");


		int status = con.getResponseCode();
		//Finally, let's read the response of the request and place it in a content String:

		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
			content.append(System.getProperty("line.separator"));
		}
		in.close();
		//To close the connection, we can use the disconnect() method:

		con.disconnect();

		//System.out.println("Prima della regex: " + content);
		String sRegex = "\\n(?s)([^@ ].*?):";


		Pattern pattern = Pattern.compile(sRegex);
		Matcher matcher = pattern.matcher(content);

		StringBuilder sResult = new StringBuilder();
		// Check all occurrences
		while (matcher.find()) {

			if (!sResult.toString().contains(matcher.group())) {

				sResult.append(matcher.group());

			}

		}
		//System.out.println("Questo è il contenuto di sResult dopo la regex: " + sResult);

		//sResult= sRegex;
		sResult = new StringBuilder(sResult.toString().replace("\n", ""));

		sResult = new StringBuilder(sResult.toString().replace(":", ","));
		String jsonPrefixes = gson.toJson(sResult.toString());
		//System.out.println("Questo è il contenuto Json alla fine: " + jsonPrefixes);

		return Response.status(Status.OK).entity(jsonPrefixes).build();
	}

	@GET
	@Path("getLanguagesFromGithub")
	public Response getLanguagesFromGithub() throws IOException {

		URL url = new URL(this.sListTtlfromGithub);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");

		//con.setRequestProperty("Content-Type", "text/trig");
		String contentType = con.getHeaderField("Content-Type");


		int status = con.getResponseCode();
		//Finally, let's read the response of the request and place it in a content String:

		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			//content.append(System.getProperty("line.separator"));
		}

		List<String> lLanguagesFromGithub = new ArrayList<String>();


		//String jsonPrefixes = gson.toJson(content);

		JSONArray jLanguagesGithub = new JSONArray(content.toString());

		for (int i = 0; i < jLanguagesGithub.length(); i++) {

			JSONObject joLanguagesGithub = jLanguagesGithub.getJSONObject(i);

			if (joLanguagesGithub.getString("name").endsWith(".ttl")){

				lLanguagesFromGithub.add(joLanguagesGithub.getString("name"));
			}
		}

		in.close();
		//To close the connection, we can use the disconnect() method:

		con.disconnect();

		String jsonFromGithub = gson.toJson(lLanguagesFromGithub);

		return Response.status(Status.OK).entity(jsonFromGithub).build();
	}


	//MODEL UPLOAD VIA API
	@POST
	@Path("postLanguagesSelectedtoFuseki")
	public Response postLanguagesSelectedtoFuseki(String json) throws IOException {
		List<String> sLanguageSelection = gson.fromJson(json, List.class);

		try {
			if(datasetIsEmpty(OntologyManager.getTRIPLESTOREENDPOINT())) {
				FileWriter myWriter = new FileWriter("AOAME.ttl");
				String sPathTtl = readFilesFromGithub(sLanguageSelection);
				uploadRDF(new File(sPathTtl), OntologyManager.getDATAENDPOINT());
			}
		} catch (Exception e) {

			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		return Response.status(Status.OK).build();
	}


	public static Response uploadRDF(File rdf, String serviceURI)
			throws IOException {

		org.apache.jena.rdf.model.Model m = ModelFactory.createDefaultModel();
		try (FileInputStream in = new FileInputStream(rdf)) {
			m.read(in, null, "TURTLE");


			// upload the resulting model
			DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(serviceURI);
			accessor.add(m);
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		return Response.status(Status.OK).build();
	}

	public static boolean datasetIsEmpty(String serviceURI) {
		// Query to check if the dataset is empty
		String queryString = "ASK { ?s ?p ?o }";
		// Set up the query execution
		try (QueryExecution qExec = QueryExecutionFactory.sparqlService(serviceURI + "/query", queryString)) {
			return !qExec.execAsk(); // execAsk returns true if the dataset contains any triples
		} catch (Exception e) {
			e.printStackTrace();
			// Handle error (e.g., log, throw a custom exception, or return a default value)
			return false; // or true, depending on how you want to handle errors
		}
	}


	public String readFilesFromGithub(List<String> sLanguageSelection)
			throws IOException {

		StringBuffer content = new StringBuffer();
		for (String element : sLanguageSelection) {
			String sPrefixForRegex = element;
			URL url = new URL(this.sRawContentTtlFromGithub + sPrefixForRegex);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");

			//con.setRequestProperty("Content-Type", "text/trig");
			String contentType = con.getHeaderField("Content-Type");
			int status = con.getResponseCode();
			//Finally, let's read the response of the request and place it in a content String:

			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {

				//if(!inputLine.contains("rdfs:comment")) {
				content.append(inputLine);
				content.append(System.getProperty("line.separator"));
				//}
			}
			con.disconnect();
		}
		String sTtlToUpload = content.toString();
		sTtlToUpload = normalizeTtlString(sTtlToUpload);

		String tempPath = "";
		try {
			java.nio.file.Path tempFile = Files.createTempFile(null, null);
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile.toFile()))) {
				bw.write(sTtlToUpload);
				tempPath = tempFile.toAbsolutePath().toString();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return tempPath;
	}

	@POST
	@Path("postTtlFromDesktop")
	public Response postLanguagesSelectedtoFuseki2(String sUrlFileIo) throws IOException {

		try {
			String sTtl = fileioToString(sUrlFileIo);
			String sPathTtl = makeTempFile(sTtl);
			uploadRDF(new File(sPathTtl), OntologyManager.getDATAENDPOINT());
			return Response.status(Status.OK).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
	}


	public static String makeTempFile(String sContentToUpload) {


		sContentToUpload = normalizeTtlString(sContentToUpload);
		//sContentToUpload= sContentToUpload.replaceAll("[\\x00-\\x1F\\x7F][^\\s.]", "");
		String tempPath = "";
		try {
			java.nio.file.Path tempFile = Files.createTempFile(null, null);
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile.toFile()))) {
				bw.write(sContentToUpload);
				tempPath = tempFile.toAbsolutePath().toString();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return tempPath;


	}


	public static String normalizeTtlString(String sTtl) {

		sTtl = sTtl.replaceAll("\\u001E", "");
		sTtl = sTtl.replaceAll("\\u001C", "");
		sTtl = sTtl.replaceAll("\\u2013", "");

		return sTtl;

	}


	public static String fileioToString(String sKey) throws IOException {

		URL url = new URL(sKey);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");

		con.setRequestProperty("Content-Type", "text/trig");
		String contentType = con.getHeaderField("Content-Type");


		int status = con.getResponseCode();
		//Finally, let's read the response of the request and place it in a content String:

		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
			content.append(System.getProperty("line.separator"));
		}
		String sContent = content.toString();
		return sContent;

	}


}
