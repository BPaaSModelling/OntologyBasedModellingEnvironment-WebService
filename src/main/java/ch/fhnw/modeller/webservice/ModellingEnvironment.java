package ch.fhnw.modeller.webservice;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import ch.fhnw.modeller.model.model.Model;
import ch.fhnw.modeller.webservice.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.MethodNotSupportedException;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import com.google.gson.Gson;

import ch.fhnw.modeller.model.graphEnvironment.Answer;
import ch.fhnw.modeller.model.metamodel.DomainElement;
import ch.fhnw.modeller.model.metamodel.GraphicalElement;
import ch.fhnw.modeller.model.metamodel.ModelingLanguage;
import ch.fhnw.modeller.model.metamodel.ModelingView;
import ch.fhnw.modeller.model.palette.PaletteCategory;
import ch.fhnw.modeller.model.palette.PaletteElement;
import ch.fhnw.modeller.model.metamodel.DatatypeProperty;
import ch.fhnw.modeller.model.metamodel.ObjectProperty;
import ch.fhnw.modeller.webservice.exception.NoResultsException;
import ch.fhnw.modeller.webservice.ontology.FormatConverter;
import ch.fhnw.modeller.webservice.ontology.NAMESPACE;
import ch.fhnw.modeller.webservice.ontology.OntologyManager;
import ch.fhnw.modeller.persistence.GlobalVariables;

import static ch.fhnw.modeller.webservice.ontology.NAMESPACE.MODEL;

@Path("/ModEnv")
public class ModellingEnvironment {
	private Gson gson = new Gson();
	private OntologyManager ontology = OntologyManager.getInstance();
	private boolean debug_properties = false;

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

		ParameterizedSparqlString deleteQuery = getDeleteModelQuery(modelId);
		ontology.insertQuery(deleteQuery);

		return Response.status(Status.OK).build();
	}

	private ParameterizedSparqlString getDeleteModelQuery(String modelId) {

		String command = String.format(
				 "DELETE {\n" +
				"\t%1$s:%2$s ?modelRel ?modelObj .\n" +
				"\t?diag ?diagRel ?diagObj .\n" +
				"\t?referencingDiag %1$s:diagramRepresentsModel %1$s:%2$s\n" +
				"}\n" +
				"WHERE {\n" +
				"\t%1$s:%2$s ?modelRel ?modelObj .\n" +
				"\tOPTIONAL { %1$s:%2$s %1$s:modelHasDiagram ?diag .\n" +
				"\t?diag ?diagRel ?diagObj } .\n" +
				"\tOPTIONAL { ?referencingDiag %1$s:diagramRepresentsModel %1$s:%2$s}\n" +
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

	@GET
	@Path("/model/{id}/diagram")
	public Response getDiagramList(@PathParam("id") String id) {

		String modelId = String.format("%s:%s", MODEL.getPrefix(), id);

		String command = String.format(
				"SELECT ?diag\n" +
				"WHERE {\n" +
				"\t%1$s %2$s:modelHasDiagram ?diag .\n" +
				"}",
				modelId,
				MODEL.getPrefix()
		);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		List<String> diagramIds = new ArrayList<>();

		while (resultSet.hasNext()) {
			QuerySolution solution = resultSet.next();
			diagramIds.add(extractIdFrom(solution, "?diag"));
		}

		List<DiagramDetailDto> diagrams = new ArrayList<>(diagramIds.size());

		diagramIds.forEach(diagramId -> {
			Map<String, String> diagramAttributes = getDiagramAttributes(diagramId);
			PaletteVisualInformationDto visualInformationDto = getPaletteVisualInformation(diagramId);

			String modelElementId = diagramAttributes.get("diagramVisualisesModelingLanguageConstructInstance").split("#")[1];
			ModelElementAttributes modelElementAttributes = getModelElementAttributesAndOptions(modelElementId);

			modelElementAttributes.getModelElementType()
					.ifPresent(elementType -> diagrams.add(
									DiagramDetailDto.from(
											diagramId,
											diagramAttributes,
											modelElementAttributes,
											elementType,
											visualInformationDto)));
		});

		String payload = gson.toJson(diagrams);

		return Response.status(Status.OK).entity(payload).build();
	}

	private PaletteVisualInformationDto getPaletteVisualInformation(String diagramId) {

		String command = String.format(
				"SELECT ?imgUrl ?cat ?fromArrow ?toArrow ?arrowStroke\n" +
				"WHERE\n" +
				"{\n" +
				"\t%1$s:%2$s %1$s:diagramInstantiatesPaletteConstruct ?po .\n" +
				"\t?po po:paletteConstructIsGroupedInPaletteCategory ?cat\n" +
				"\tOPTIONAL { ?po po:paletteConstructHasModelImage ?imgUrl }\n" +
				"\tOPTIONAL { ?po po:paletteConnectorConfiguresFromArrowHead ?fromArrow }\n" +
				"\tOPTIONAL { ?po po:paletteConnectorConfiguresToArrowHead ?toArrow }\n" +
				"\tOPTIONAL { ?po po:paletteConnectorConfiguresArrowStroke ?arrowStroke }\n" +
				"}",
				MODEL.getPrefix(),
				diagramId);

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

	@GET
	@Path("/model/{modelId}/diagram/{id}")
	public Response getDiagram(	@PathParam("modelId") String modelId,
							  	@PathParam("id") String diagramId) {

		DiagramDetailDto dto = getDiagramDetail(modelId, diagramId);

		String payload = gson.toJson(dto);

		return Response.status(Status.OK).entity(payload).build();
	}

	private DiagramDetailDto getDiagramDetail(String modelId, String diagramId) {
		String modelIdentifier = String.format("%s:%s", MODEL.getPrefix(), modelId);

		Map<String, String> diagramAttributes = getDiagramAttributes(diagramId);
		String modelElement = diagramAttributes.get("diagramVisualisesModelingLanguageConstructInstance");
		String modelElementId = modelElement.split("#")[1];
		ModelElementAttributes modelElementAttributes = getModelElementAttributesAndOptions(modelElementId);

		PaletteVisualInformationDto visualInformationDto = getPaletteVisualInformation(diagramId);

		return modelElementAttributes.getModelElementType()
				.map(elementType -> DiagramDetailDto.from(diagramId, diagramAttributes, modelElementAttributes, elementType, visualInformationDto))
				.orElse(null);
	}

	private Optional<ModelElementType> getModelElementType(String modelElementId) {

		String command = String.format(
				"SELECT ?type ?class \n" +
				"WHERE { \n" +
				"\t{\n" +
				"\t%1$s:%2$s rdf:type %1$s:ModelConstructInstance . \n" +
				"\t%1$s:%2$s rdfs:subClassOf* ?type . \n" +
				"\t?type rdfs:subClassOf lo:ModelingLanguageConstruct \n" +
				"\t}\n" +
				"\tUNION\n" +
				"\t{\n" +
				"\t%1$s:%2$s rdf:type %1$s:ModelConstructInstance . \n" +
				"\t%1$s:%2$s rdf:type ?class .\n" +
				"\t?class rdfs:subClassOf* ?type . \n" +
				"\t?type rdfs:subClassOf lo:ModelingLanguageConstruct \n" +
				"\t}\n" +
				"}",
				MODEL.getPrefix(),
				modelElementId
				);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		if (resultSet.hasNext()) {
			QuerySolution next = resultSet.next();
			String type = extractIdFrom(next, "?type");
			String directType = extractIdFrom(next, "?class");

			if (type != null) {
				ModelElementType modelElementType = new ModelElementType();
				modelElementType.setType(type);
				modelElementType.setInstantiationType(directType != null ? InstantiationTargetType.Instance : InstantiationTargetType.Class);

				return Optional.of(modelElementType);
			}
		}

		return Optional.empty();
	}

	private Map<String, String> getDiagramAttributes(String diagramId) {

		Map<String, String> attributes = new HashMap<>();

		String command = String.format(
				"SELECT *\n" +
				"WHERE {\n" +
				"\t%1$s:%2$s ?rel ?relValue .\n" +
				"\t%1$s:%2$s rdf:type %1$s:Diagram\n" +
				"}",
				MODEL.getPrefix(),
				diagramId
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
				"\t%1$s:%2$s rdf:type %1$s:ModelConstructInstance .\n" +
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

	private ModelElementAttributes getModelElementAttributesAndOptions(String modelElementId) {

		Optional<ModelElementType> elementTypeOpt = getModelElementType(modelElementId);

		if (!elementTypeOpt.isPresent()) return null;

		String instanceCreationBit = "\t\t%1$s:%2$s rdf:type ?mloConcept .\n";
		String classCreationBit = 	"\t\t%1$s:%2$s rdf:type owl:Class .\n" +
									"\t\t%1$s:%2$s rdfs:subClassOf ?mloConcept .\n";

		String creationBit = elementTypeOpt.get().getInstantiationType() == InstantiationTargetType.Class ? classCreationBit : instanceCreationBit;

		String command = String.format(
				"SELECT ?objProp ?range ?instanceValue ?classValue\n" +
				"WHERE {\n" +
				creationBit +
				"\t\t?mloConcept rdfs:subClassOf* ?relTarget .\n" +
				"\n" +
				"\t\t?objProp rdf:type owl:ObjectProperty .\n" +
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
				"\t\t\t?superObjProp %1$s:objectPropertyIsShownInModel true .\n" +
				"\n" +
				"\t\t\tFILTER NOT EXISTS {\n" +
				"\t\t\t\t?subObjProp rdfs:subPropertyOf+ ?superObjProp .\n" +
				"\t\t\t\t?subObjProp %1$s:objectPropertyIsShownInModel false .\n" +
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
				ModelElementAttribute value = new ModelElementAttribute();
				value.setRelation(relationDto.getRelation());
				value.setRelationPrefix(relationDto.getRelationPrefix());
				value.setValue(actualValue.split("#")[1]);
				value.setValuePrefix(GlobalVariables.getNamespaceMap().get(actualValue.split("#")[0]));

				values.add(value);
			}
		}

        List<String> referencingDiagrams = getReferencingDiagramIds(modelElementId);

        return new ModelElementAttributes(
				options,
				values,
				elementTypeOpt.get().getType(),
                elementTypeOpt.get().getInstantiationType(),
                referencingDiagrams);
	}

    private List<String> getReferencingDiagramIds(String modelElementId) {
        String incomingReferencesCommand = String.format(
                "SELECT ?diag\n" +
				"WHERE { \n" +
				"\t?diag %1$s:diagramVisualisesModelingLanguageConstructInstance %1$s:%2$s .\n" +
				"}",
				MODEL.getPrefix(),
				modelElementId);

        List<String> referencingDiagrams = new ArrayList<>();

        ParameterizedSparqlString incomingReferencesQuery = new ParameterizedSparqlString(incomingReferencesCommand);
        ResultSet incomingReferencesResultSet = ontology.query(incomingReferencesQuery).execSelect();
        while (incomingReferencesResultSet.hasNext()) {
            QuerySolution next = incomingReferencesResultSet.next();
            String diagram = extractIdFrom(next, "?diag");
            referencingDiagrams.add(diagram);
        }
        return referencingDiagrams;
    }

    @PUT
	@Path("/model/{modelId}/diagram")
	public Response createDiagram(@PathParam("modelId") String modelId,
								  String json) {

		Gson gson = new Gson();
		DiagramCreationDto diagramCreationDto = gson.fromJson(json, DiagramCreationDto.class);

		if (diagramCreationDto.getModelingLanguageConstructInstance() == null) {
			createDiagramForNewModelElement(modelId, diagramCreationDto);
		} else {
			ParameterizedSparqlString query = getDiagramCreationQuery(diagramCreationDto, modelId, diagramCreationDto.getModelingLanguageConstructInstance());
			ontology.insertQuery(query);
		}

		Object diagram = getDiagram(modelId, diagramCreationDto.getUuid()).getEntity();
		return Response.status(Status.CREATED).entity(diagram).build();
	}

	private ParameterizedSparqlString getDiagramAndModelElementCreationQuery(DiagramCreationDto diagramCreationDto, String modelId, String elementId) {
		String instanceCreationBit = "	%7$s:%1$s rdf:type ?type .\n";
		String classCreationBit = 	"	%7$s:%1$s rdf:type owl:Class .\n" +
									"	%7$s:%1$s rdfs:subClassOf ?type .\n";

		String creationBit = diagramCreationDto.getInstantiationType() == InstantiationTargetType.Class ? classCreationBit : instanceCreationBit;

		String command = String.format(
				"INSERT {\n" +
						creationBit +
						"	%7$s:%1$s rdf:type %7$s:ModelConstructInstance .\n" +
						"	%7$s:%1$s lo:elementIsMappedWithDOConcept ?concept .\n" +
						"	%7$s:%2$s rdf:type %7$s:Diagram .\n" +
						"	%7$s:%2$s %7$s:diagramPositionsOnCoordinateX %5$s .\n" +
						"	%7$s:%2$s %7$s:diagramPositionsOnCoordinateY %6$s .\n" +
						"	%7$s:%2$s %7$s:diagramHasLength ?height .\n" +
						"	%7$s:%2$s %7$s:diagramHasWidth ?width .\n" +
						"	%7$s:%2$s %7$s:diagramInstantiatesPaletteConstruct po:%3$s .\n" +
						"	%7$s:%2$s %7$s:diagramVisualisesModelingLanguageConstructInstance %7$s:%1$s .\n" +
						"	%7$s:%2$s rdfs:label \"%8$s\" .\n" +
						"	%7$s:%4$s %7$s:modelHasDiagram %7$s:%2$s .\n" +
						"}" +
						"WHERE {" +
						"	po:%3$s po:paletteConstructIsRelatedToModelingLanguageConstruct ?type .\n" +
						"	po:%3$s po:paletteConstructHasHeight ?height .\n" +
						"	po:%3$s po:paletteConstructHasWidth ?width .\n" +
						"	OPTIONAL { ?type lo:elementIsMappedWithDOConcept ?concept }\n" +
						"}",
				elementId,
				diagramCreationDto.getUuid(),
				diagramCreationDto.getPaletteConstruct(),
				modelId,
				diagramCreationDto.getX(),
				diagramCreationDto.getY(),
				MODEL.getPrefix(),
				diagramCreationDto.getLabel()
		);

		return new ParameterizedSparqlString(command);
	}

	private void createDiagramForNewModelElement(String modelId, DiagramCreationDto diagramCreationDto) {
		Optional<String> mappedModelingLanguageConstruct = getMappedModelingLanguageConstruct(diagramCreationDto.getPaletteConstruct());

		if (!mappedModelingLanguageConstruct.isPresent()) {
			throw new IllegalArgumentException("Palette Construct must be related to a Modeling Language Construct");
		}

		String elementId = String.format("%s_%s", mappedModelingLanguageConstruct.get(), UUID.randomUUID().toString());

		ParameterizedSparqlString query = getDiagramAndModelElementCreationQuery(diagramCreationDto, modelId, elementId);
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

		String diagramId = String.format("%s_Diagram_%s",
				connectionCreationDto.getPaletteConstruct(),
				connectionCreationDto.getUuid());

		String elementId = String.format("%s_%s", mappedModelingLanguageConstruct.get(), UUID.randomUUID().toString());

		String command = getConnectionCreationCommand(connectionCreationDto, modelId, diagramId, elementId);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ontology.insertQuery(query);

		Object diagram = getDiagram(modelId, diagramId).getEntity();
		return Response.status(Status.CREATED).entity(diagram).build();
	}

	@DELETE
	@Path("/model/{modelId}/diagram/{diagramId}")
	public Response deleteDiagram(@PathParam("modelId") String modelId,
								  @PathParam("diagramId") String diagramId) {

		DiagramDetailDto diagramDetail = getDiagramDetail(modelId, diagramId);
		String modelingLanguageConstructInstance = diagramDetail.getModelingLanguageConstructInstance();

		ParameterizedSparqlString deleteQuery = getDeleteDiagramQuery(diagramId);
		ontology.insertQuery(deleteQuery);

		if (StringUtils.isNotBlank(modelingLanguageConstructInstance)
				&& !isModelingLanguageConstructLinkedInAnotherModel(modelingLanguageConstructInstance)) {
			deleteModelElementInstance(modelingLanguageConstructInstance);
		}

		return Response.status(Status.OK).build();
	}

	private boolean isModelingLanguageConstructLinkedInAnotherModel(String instanceId) {

		String command = String.format(
				"SELECT *\n" +
				"WHERE {\n" +
				"    ?diagram %1$s:diagramVisualisesModelingLanguageConstructInstance %1$s:%2$s\n" +
				"}",
				MODEL.getPrefix(),
				instanceId
		);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		return resultSet.hasNext();
	}

	@PUT
	@Path("/model/{modelId}/diagram/{diagramId}")
	public Response updateDiagram(@PathParam("modelId") String modelId,
								  @PathParam("diagramId") String diagramId,
								  String json) {

		DiagramDetailDto diagram = getDiagramDetail(modelId, diagramId);

		if (diagram == null) {
			return Response.status(Status.NOT_FOUND).build();
		}

		Gson gson = new Gson();
		DiagramDetailDto diagramToBeStored = gson.fromJson(json, DiagramDetailDto.class);

		List<ParameterizedSparqlString> queries = new ArrayList<>();

		ParameterizedSparqlString deleteQuery = getDeleteDiagramDataQuery(diagramId);
		queries.add(deleteQuery);

		ParameterizedSparqlString insertQuery = getInsertDiagramDataQuery(diagramId, diagramToBeStored);
		queries.add(insertQuery);

		if (diagramToBeStored.hasOptionalValues()) {
			ParameterizedSparqlString optInsertQuery = getInsertOptionalDiagramDataQuery(diagramId, diagramToBeStored);
			queries.add(optInsertQuery);
		}

		if (diagram.getModelElementAttributes() != null &&
			diagram.getModelElementAttributes().getValues() != null &&
			!diagram.getModelElementAttributes().getValues().isEmpty()) {

			ParameterizedSparqlString deleteQueryElement = getDeleteModelElementDataQuery(diagram.getModelingLanguageConstructInstance(), diagram.getModelElementAttributes().getValues());
			queries.add(deleteQueryElement);
		}

		if (diagram.getModelingLanguageConstructInstance().equals(diagramToBeStored.getModelingLanguageConstructInstance()) &&
			diagramToBeStored.getModelElementAttributes() != null &&
			diagramToBeStored.getModelElementAttributes().getValues() != null &&
			!diagramToBeStored.getModelElementAttributes().getValues().isEmpty()) {

			Optional<ParameterizedSparqlString> insertModelElementDataQuery = getInsertModelElementDataQuery(diagram.getModelingLanguageConstructInstance(), diagramToBeStored.getModelElementAttributes().getValues());
			insertModelElementDataQuery.ifPresent(queries::add);
		}

		if ("ModelingContainer".equals(diagram.getModelElementType())) {
			ParameterizedSparqlString deleteQueryElement = getDeleteContainedModelElementsQuery(diagram.getModelingLanguageConstructInstance());
			queries.add(deleteQueryElement);

			if (diagramToBeStored.getContainedDiagrams() != null && !diagramToBeStored.getContainedDiagrams().isEmpty()) {
				ParameterizedSparqlString insertQueryElement = getInsertContainedModelElementsQuery(diagramToBeStored.getModelingLanguageConstructInstance(), diagramToBeStored.getContainedDiagrams());
				queries.add(insertQueryElement);
			}
		}

		ontology.insertMultipleQueries(queries);

		return Response.status(Status.CREATED).entity(getDiagram(modelId, diagramId).getEntity()).build();
	}

	private ParameterizedSparqlString getInsertContainedModelElementsQuery(String containerInstance, List<String> containedDiagrams) {

		StringBuilder command = new StringBuilder("INSERT DATA {\n");
		containedDiagrams.forEach(diagram -> {
			command.append(String.format(
					"\t%1$s:%2$s %1$s:modelingContainerContainsModelingLanguageConstruct %1$s:%3$s .\n",
					MODEL.getPrefix(),
					containerInstance,
					diagram));
		});
		command.append("}");

		return new ParameterizedSparqlString(command.toString());
	}

	private ParameterizedSparqlString getDeleteContainedModelElementsQuery(String modelingLanguageConstructInstance) {

		String command = String.format(
				"DELETE\n" +
				"{\n" +
				"\t%1$s:%2$s %1$s:modelingContainerContainsModelingLanguageConstruct ?o\n" +
				"}\n" +
				"WHERE {\n" +
				"\t%1$s:%2$s %1$s:modelingContainerContainsModelingLanguageConstruct ?o\n" +
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
					" %1$s:%2$s %3$s:%4$s %5$s:%6$s . \n",
					MODEL.getPrefix(),
					modelingLanguageConstruct,
					modelElementAttribute.getRelationPrefix(),
					modelElementAttribute.getRelation(),
					modelElementAttribute.getValuePrefix(),
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

	private ParameterizedSparqlString getInsertOptionalDiagramDataQuery(String diagramId, DiagramDetailDto diagram) {

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("INSERT DATA {\n");

		if (diagram.getNote() != null) {
			queryBuilder.append(String.format(
					"\t%1$s:%2$s %1$s:diagramHasNote \"%3$s\" .\n",
					MODEL.getPrefix(),
					diagramId,
					diagram.getNote()));
		}

		if (diagram.getLabel() != null) {
			queryBuilder.append(String.format(
					"\t%1$s:%2$s rdfs:label \"%3$s\" .\n",
					MODEL.getPrefix(),
					diagramId,
					diagram.getLabel()));
		}

		if (diagram.getDiagramRepresentsModel() != null) {
			queryBuilder.append(String.format(
					"\t%1$s:%2$s %1$s:diagramRepresentsModel %1$s:%3$s .\n",
					MODEL.getPrefix(),
					diagramId,
					diagram.getDiagramRepresentsModel()));
		}

		queryBuilder.append("}");

		return new ParameterizedSparqlString(queryBuilder.toString());
	}

	private ParameterizedSparqlString getInsertDiagramDataQuery(String diagramId, DiagramDetailDto diagram) {
		String command = String.format(
				"INSERT DATA {\n" +
				"\t%1$s:%2$s %1$s:diagramPositionsOnCoordinateX %3$s .\n" +
				"\t%1$s:%2$s %1$s:diagramPositionsOnCoordinateY %4$s .\n" +
				"\t%1$s:%2$s %1$s:diagramHasLength %5$s .\n" +
				"\t%1$s:%2$s %1$s:diagramHasWidth %6$s .\n" +
				"\t%1$s:%2$s %1$s:diagramInstantiatesPaletteConstruct %7$s .\n" +
				"\t%1$s:%2$s %1$s:diagramVisualisesModelingLanguageConstructInstance %1$s:%8$s .\n" +
				"} ",
				MODEL.getPrefix(),
				diagramId,
				diagram.getX(),
				diagram.getY(),
				diagram.getHeight(),
				diagram.getWidth(),
				diagram.getPaletteConstruct(),
				diagram.getModelingLanguageConstructInstance()
		);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("INSERT DATA {\n");

		if (diagram.getNote() != null) {
			queryBuilder.append(String.format(
					"\t%1$s:%2$s %1$s:diagramHasNote \"%3$s\" .\n",
					MODEL.getPrefix(),
					diagramId,
					diagram.getNote()));
		}

		if (diagram.getLabel() != null) {
			queryBuilder.append(String.format(
					"\t%1$s:%2$s rdfs:label \"%3$s\" .\n",
					MODEL.getPrefix(),
					diagramId,
					diagram.getLabel()));
		}

		if (diagram.getDiagramRepresentsModel() != null) {
			queryBuilder.append(String.format(
					"\t%1$s:%2$s %1$s:diagramRepresentsModel %1$s:%3$s .\n",
					MODEL.getPrefix(),
					diagramId,
					diagram.getDiagramRepresentsModel()));
		}

		queryBuilder.append("}");

		return new ParameterizedSparqlString(command);
	}

	private ParameterizedSparqlString getDeleteDiagramQuery(String diagramId) {
		String command = String.format(
				"DELETE {\n" +
						"\t%1$s:%2$s %1$s:diagramPositionsOnCoordinateX ?x .\n" +
						"\t%1$s:%2$s %1$s:diagramPositionsOnCoordinateY ?y .\n" +
						"\t%1$s:%2$s %1$s:diagramHasLength ?h .\n" +
						"\t%1$s:%2$s %1$s:diagramHasWidth ?w .\n" +
						"\t%1$s:%2$s %1$s:diagramInstantiatesPaletteConstruct ?po .\n" +
						"\t%1$s:%2$s %1$s:diagramVisualisesModelingLanguageConstructInstance ?mlo .\n" +
						"\t%1$s:%2$s %1$s:diagramHasNote ?note .\n" +
						"\t%1$s:%2$s rdfs:label ?label .\n" +
						"\t%1$s:%2$s %1$s:diagramRepresentsModel ?model .\n" +
						"\t?parentModel %1$s:modelHasDiagram %1$s:%2$s .\n" +
						"} WHERE {\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:diagramPositionsOnCoordinateX ?x }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:diagramPositionsOnCoordinateY ?y }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:diagramHasLength ?h }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:diagramHasWidth ?w }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:diagramInstantiatesPaletteConstruct ?po }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:diagramVisualisesModelingLanguageConstructInstance ?mlo }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:diagramHasNote ?note }\n" +
						"\tOPTIONAL { %1$s:%2$s rdfs:label ?label }\n" +
						"\tOPTIONAL { %1$s:%2$s %1$s:diagramRepresentsModel ?model }\n" +
						"\t?parentModel %1$s:modelHasDiagram %1$s:%2$s .\n" +
						"} ",
				MODEL.getPrefix(),
				diagramId
		);

		return new ParameterizedSparqlString(command);
	}

	private ParameterizedSparqlString getDeleteDiagramDataQuery(String diagramId) {
		String command = String.format(
				"DELETE {\n" +
				"\t%1$s:%2$s %1$s:diagramPositionsOnCoordinateX ?x .\n" +
				"\t%1$s:%2$s %1$s:diagramPositionsOnCoordinateY ?y .\n" +
				"\t%1$s:%2$s %1$s:diagramHasLength ?h .\n" +
				"\t%1$s:%2$s %1$s:diagramHasWidth ?w .\n" +
				"\t%1$s:%2$s %1$s:diagramInstantiatesPaletteConstruct ?po .\n" +
				"\t%1$s:%2$s %1$s:diagramVisualisesModelingLanguageConstructInstance ?mlo .\n" +
				"\t%1$s:%2$s %1$s:diagramHasNote ?note .\n" +
				"\t%1$s:%2$s rdfs:label ?label .\n" +
				"\t%1$s:%2$s %1$s:diagramRepresentsModel ?model .\n" +
				"} WHERE {\n" +
				"\tOPTIONAL { %1$s:%2$s %1$s:diagramPositionsOnCoordinateX ?x }\n" +
				"\tOPTIONAL { %1$s:%2$s %1$s:diagramPositionsOnCoordinateY ?y }\n" +
				"\tOPTIONAL { %1$s:%2$s %1$s:diagramHasLength ?h }\n" +
				"\tOPTIONAL { %1$s:%2$s %1$s:diagramHasWidth ?w }\n" +
				"\tOPTIONAL { %1$s:%2$s %1$s:diagramInstantiatesPaletteConstruct ?po }\n" +
				"\tOPTIONAL { %1$s:%2$s %1$s:diagramVisualisesModelingLanguageConstructInstance ?mlo }\n" +
				"\tOPTIONAL { %1$s:%2$s %1$s:diagramHasNote ?note }\n" +
				"\tOPTIONAL { %1$s:%2$s rdfs:label ?label }\n" +
				"\tOPTIONAL { %1$s:%2$s %1$s:diagramRepresentsModel ?model }\n" +
				"} ",
				MODEL.getPrefix(),
				diagramId
		);

		return new ParameterizedSparqlString(command);
	}

	private ParameterizedSparqlString getDiagramCreationQuery(DiagramCreationDto diagramCreationDto, String modelId, String elementId) {

		String command = String.format(
				"INSERT DATA {\n" +
				"	%1$s:%2$s rdf:type %1$s:Diagram .\n" +
				"	%1$s:%2$s %1$s:diagramPositionsOnCoordinateX %6$s .\n" +
				"	%1$s:%2$s %1$s:diagramPositionsOnCoordinateY %7$s .\n" +
				"	%1$s:%2$s %1$s:diagramHasWidth %8$s .\n" +
				"	%1$s:%2$s %1$s:diagramHasLength %9$s .\n" +
				"	%1$s:%2$s %1$s:diagramInstantiatesPaletteConstruct %5$s .\n" +
				"	%1$s:%2$s %1$s:diagramVisualisesModelingLanguageConstructInstance %1$s:%4$s .\n" +
				"	%1$s:%3$s %1$s:modelHasDiagram %1$s:%2$s .\n",
				MODEL.getPrefix(),
				diagramCreationDto.getUuid(),
				modelId,
				elementId,
				diagramCreationDto.getPaletteConstruct(),
				diagramCreationDto.getX(),
				diagramCreationDto.getY(),
				diagramCreationDto.getW(),
				diagramCreationDto.getH()
		);

		StringBuilder commandBuilder = new StringBuilder(command);

		if (diagramCreationDto.getNote() != null) {
			commandBuilder.append(String.format("	%1$s:%2$s %1$s:diagramHasNote %1$s:%3$s .\n",
					MODEL.getPrefix(),
					diagramCreationDto.getUuid(),
					diagramCreationDto.getNote()));
		}

		if (diagramCreationDto.getDiagramRepresentsModel() != null) {
			commandBuilder.append(String.format("	%1$s:%2$s %1$s:diagramRepresentsModel %1$s:%3$s .\n",
					MODEL.getPrefix(),
					diagramCreationDto.getUuid(),
					diagramCreationDto.getDiagramRepresentsModel()));
		}

		if (diagramCreationDto.getLabel() != null) {
			commandBuilder.append(String.format("	%1$s:%2$s rdfs:label \"%3$s\" .\n",
					MODEL.getPrefix(),
					diagramCreationDto.getUuid(),
					diagramCreationDto.getLabel()));
		}

		commandBuilder.append("}");

		return new ParameterizedSparqlString(commandBuilder.toString());
	}

	private String getConnectionCreationCommand(ConnectionCreationDto connectionCreationDto, String modelId, String id, String elementId) {

		String instanceCreationBit = "	%7$s:%1$s rdf:type ?type .\n";
		String classCreationBit = 	"	%7$s:%1$s rdf:type owl:Class .\n" +
									"	%7$s:%1$s rdfs:subClassOf ?type .\n";

		String creationBit = connectionCreationDto.getInstantiationType() == InstantiationTargetType.Class ? instanceCreationBit : classCreationBit;

		return String.format(
				"INSERT {\n" +
				creationBit +
				"	%7$s:%1$s rdf:type %7$s:ModelConstructInstance .\n" +
				"	%7$s:%1$s lo:elementIsMappedWithDOConcept ?concept .\n" +
				"	%7$s:%1$s lo:modelingRelationHasSourceModelingElement ?fromInstance .\n" +
				"	%7$s:%1$s lo:modelingRelationHasTargetModelingElement ?toInstance .\n" +
				"	%7$s:%2$s rdf:type %7$s:Diagram .\n" +
				"	%7$s:%2$s %7$s:diagramPositionsOnCoordinateX %5$s .\n" +
				"	%7$s:%2$s %7$s:diagramPositionsOnCoordinateY %6$s .\n" +
				"	%7$s:%2$s %7$s:diagramHasLength ?height .\n" +
				"	%7$s:%2$s %7$s:diagramHasWidth ?width .\n" +
				"	%7$s:%2$s %7$s:diagramInstantiatesPaletteConstruct po:%3$s .\n" +
				"	%7$s:%2$s %7$s:diagramVisualisesModelingLanguageConstructInstance %7$s:%1$s .\n" +
				"	%7$s:%4$s %7$s:modelHasDiagram %7$s:%2$s .\n" +
				"}" +
				"WHERE {" +
				"	po:%3$s po:paletteConstructIsRelatedToModelingLanguageConstruct ?type .\n" +
				"	po:%3$s po:paletteConstructHasHeight ?height .\n" +
				"	po:%3$s po:paletteConstructHasWidth ?width .\n" +
				"   %7$s:%8$s rdf:type %7$s:Diagram . \n" +
				"   %7$s:%9$s rdf:type %7$s:Diagram . \n" +
				"   %7$s:%8$s %7$s:diagramVisualisesModelingLanguageConstructInstance ?fromInstance . \n" +
				"   %7$s:%9$s %7$s:diagramVisualisesModelingLanguageConstructInstance ?toInstance . \n" +
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
				"\t\t?instance rdf:type %1$s:ModelConstructInstance .\n" +
				"\t\t?instance ?relation ?object\n" +
				"\n" +
				"\t\tFILTER NOT EXISTS\n" +
				"\t\t{\n" +
				"\t\t\t?diagram %1$s:diagramVisualisesModelingLanguageConstructInstance ?instance\n" +
				"\t\t}\n" +
				"\t}\n" +
				"\tUNION\n" +
				"\t{\n" +
				"\t\t?instance rdf:type %1$s:ModelConstructInstance .\n" +
				"\t\t?subject ?relation ?instance\n" +
				"\n" +
				"\t\tFILTER NOT EXISTS\n" +
				"\t\t{\n" +
				"\t\t\t?diagram %1$s:diagramVisualisesModelingLanguageConstructInstance ?instance\n" +
				"\t\t}\n" +
				"\t}\n" +
				"}\n",
				MODEL.getPrefix()
		);

		ParameterizedSparqlString query = new ParameterizedSparqlString(command);
		ResultSet resultSet = ontology.query(query).execSelect();

		Map<String, Map<String, String>> attributes = new HashMap<>();

		while(resultSet.hasNext()) {
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

		String payload = gson.toJson(new Options(instances, classes));

		return Response.ok(payload).build();
	}

	@GET
	@Path("/getModelingLanguages")
	public Response getModelingLanguages() {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested modeling languages" );
		System.out.println("####################<end>####################");
		ArrayList<ModelingLanguage> all_modeling_languages = new ArrayList<ModelingLanguage>();

		try {
			all_modeling_languages = queryAllModelingLangugages();

			if (debug_properties){
				for (int index = 0; index < all_modeling_languages.size(); index++){
					System.out.println("Langugage "+index+": ");
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}




		String json = gson.toJson(all_modeling_languages);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " +json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}

	private ArrayList<ModelingLanguage> queryAllModelingLangugages() throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<ModelingLanguage> result = new ArrayList<ModelingLanguage>();

		queryStr.append("SELECT ?element ?label ?hasModelingView ?viewIsPartOfModelingLanguage ?type WHERE {");
		queryStr.append("?element rdf:type ?type . FILTER(?type IN (lo:ModelingLanguage)) .");
		queryStr.append("?element rdfs:label ?label . ");
		queryStr.append("OPTIONAL {?element lo:hasModelingView ?hasModelingView }.");	//not needed, remove	
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
				if (soln.get("?hasModelingView") != null){
					tempModelingLanguage.setHasModelingView(FormatConverter.ParseOntologyBoolean(soln.get("?hasModelingView").toString()));
				}
				if (soln.get("?viewIsPartOfModelingLanguage") != null){
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
		System.out.println("/requested modeling languages" );
		System.out.println("####################<end>####################");
		ArrayList<ModelingView> all_modeling_views = new ArrayList<ModelingView>();

		try {
			all_modeling_views = queryAllModelingViews(langId);

			if (debug_properties){
				for (int index = 0; index < all_modeling_views.size(); index++){
					System.out.println("View "+index+": ");
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}




		String json = gson.toJson(all_modeling_views);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " +json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}

	private ArrayList<ModelingView> queryAllModelingViews(String langId) throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<ModelingView> result = new ArrayList<ModelingView>();

		queryStr.append("SELECT ?element ?label ?isMainModelingView ?viewIsPartOfModelingLanguage ?type WHERE {");
		//queryStr.append("?element rdf:type ?type . FILTER(?type IN (lo:ModelingView)) .");
		queryStr.append("?element rdfs:label ?label .");
		queryStr.append("?element lo:viewIsPartOfModelingLanguage "+ langId +" ."); 
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
				if (soln.get("?isMainModelingView") != null){
					tempModelingView.setMainModelingView(FormatConverter.ParseOntologyBoolean(soln.get("?isMainModelingView").toString()));
				}
				if (soln.get("?viewIsPartOfModelingLanguage") != null){
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
		System.out.println("/requested palette elements" );
		System.out.println("####################<end>####################");
		ArrayList<PaletteElement> all_palette_elements = new ArrayList<PaletteElement>();

		try {
			all_palette_elements = queryAllPaletteElements();

			if (debug_properties){
				for (int index = 0; index < all_palette_elements.size(); index++){
					System.out.println("Element "+index+": ");
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}




		String json = gson.toJson(addChildElements(all_palette_elements));
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " +json);
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

				if (soln.get("?backgroundColor") != null){
					tempPaletteElement.setBackgroundColor(soln.get("?backgroundColor").toString());
				}

				if (soln.get("?height") != null){
					tempPaletteElement.setHeight(FormatConverter.ParseOntologyInteger(soln.get("?height").toString()));
				}
				/*if (soln.get("?iconPosition") != null){
					tempPaletteElement.setIconPosition(soln.get("?iconPosition").toString());
				}
				if (soln.get("?iconURL") != null){
					tempPaletteElement.setIconURL(soln.get("?iconURL").toString());
				}*/
				if (soln.get("?imageURL") != null){
					tempPaletteElement.setImageURL(soln.get("?imageURL").toString());
				}
				/*if (soln.get("?labelPosition") != null){
					tempPaletteElement.setLabelPosition(soln.get("?labelPosition").toString());
				}*/
				if (soln.get("?shape") != null){
					tempPaletteElement.setShape(soln.get("?shape").toString());
				}
				if (soln.get("?thumbnailURL") != null){
					tempPaletteElement.setThumbnailURL(soln.get("?thumbnailURL").toString());
				}
				if (soln.get("?width") != null){
					tempPaletteElement.setWidth(FormatConverter.ParseOntologyInteger(soln.get("?width").toString()));
				}
				if (soln.get("?categoryLabel") != null){
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
				if (soln.get("?comment") != null){
					tempPaletteElement.setComment(soln.get("?comment").toString());
				}
				if (soln.get("?parent") != null){
					tempPaletteElement.setParentElement(soln.get("?parent").toString());
					//Read properties of the parent element here
				}

				if (soln.get("?arrowStroke") != null){
					tempPaletteElement.setArrowStroke(extractIdFrom(soln, "?arrowStroke"));
				}

				if (soln.get("?toArrow") != null){
					tempPaletteElement.setToArrow(extractIdFrom(soln, "?toArrow"));
				}

				if (soln.get("?fromArrow") != null){
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
		System.out.println("/requested palette categories" );
		System.out.println("####################<end>####################");
		ArrayList<PaletteCategory> all_palette_categories = new ArrayList<PaletteCategory>();

		try {
			all_palette_categories = queryAllPaletteCategories(viewId);

			if (debug_properties){
				for (int index = 0; index < all_palette_categories.size(); index++){
					System.out.println("Category "+index+": ");
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(all_palette_categories);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " +json);
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
				System.out.println("idSuffix: "+idSuffix);
				tempPaletteCategory.setIdSuffix(idSuffix);
				tempPaletteCategory.setLabel(soln.get("?label").toString());

				if (soln.get("?orderNumber") != null){
					tempPaletteCategory.setOrderNumber(FormatConverter.ParseOntologyInteger(soln.get("?orderNumber").toString()));
				}
				if (soln.get("?hidden") != null){
					tempPaletteCategory.setHiddenFromPalette((FormatConverter.ParseOntologyBoolean(soln.get("?hidden").toString())));
				}

				result.add(tempPaletteCategory);
			}
		}
		qexec.close();
		return result;
	}
	

	private ArrayList<PaletteElement> addChildElements(ArrayList<PaletteElement> all_palette_elements){
		ArrayList<PaletteElement> parentList = new ArrayList<PaletteElement>();
		//System.out.println("pre: " + all_palette_elements.size());
		for (int i = 0; i < all_palette_elements.size(); i++){
			if (all_palette_elements.get(i).getParentElement() == null){
				parentList.add(all_palette_elements.get(i));
			}
		}
		//System.out.println("post: " + all_palette_elements.size());
		//System.out.println("Number of parents: " + parentList.size());
		if (parentList.size() > 0){
			for (int i = 0; i < parentList.size();i++){
				if (parentList.get(i).getChildElements().size() == 0){
					//System.out.println("=========");
					addChilds(parentList.get(i), all_palette_elements);
					//System.out.println("1. Analysing " + all_palette_elements.get(i).getId());
				}
			}
		}
		return parentList;
	}

	private void addChilds(PaletteElement parent, ArrayList<PaletteElement> list){
		ArrayList<PaletteElement> childList = getChildren(parent, list);
		for (int i = 0; i < childList.size(); i++){
			//System.out.println("3. Adding child of " + list.get(i).getId());
			parent.getChildElements().add(childList.get(i));
			addChilds(childList.get(i), list);
		}
	}

	private ArrayList<PaletteElement> getChildren(PaletteElement parent, ArrayList<PaletteElement> list){
		ArrayList<PaletteElement> result = new ArrayList<PaletteElement>();
		for (int i = 0; i < list.size(); i++){
			if (list.get(i).getParentElement() != null &&
					list.get(i).getParentElement().equals(parent.getId()) && parent.getPaletteCategory().equals(list.get(i).getPaletteCategory())){
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
		System.out.println("/element received: " +json);

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

		System.out.println("/element received: " +json);

		Gson gson = new Gson();
		PaletteElement pElement = gson.fromJson(json, PaletteElement.class);
		//pElement.setClassType("http://fhnw.ch/modelingEnvironment/LanguageOntology#PaletteElement");

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		System.out.println("test: "+pElement.getUuid());
		querStr.append("INSERT DATA {");
		System.out.println("    Element ID: " + pElement.getUuid());
		querStr.append("po:" + pElement.getUuid()  +" rdf:type " + "<http://fhnw.ch/modelingEnvironment/PaletteOntology#PaletteElement>" + " ;");
		/*System.out.println("    Element Type: " + pElement.getClassType());
			querStr.append("lo:graphicalElementClassType \"" + "<"+pElement.getClassType()+">" +"\" ;");*/
		System.out.println("    Element Label: "+ pElement.getLabel());
		querStr.append("rdfs:label \"" +pElement.getLabel() +"\" ;");
		System.out.println("    Element Hidden property: "+ pElement.getHiddenFromPalette());
		querStr.append("po:paletteConstructIsHiddenFromPalette " + pElement.getHiddenFromPalette() +" ;");
		System.out.println("    Element Parent: "+ pElement.getParentElement());
		querStr.append("po:paletteConstructHasParentPaletteConstruct <" + pElement.getParentElement() +"> ;");
		System.out.println("    Element Category: "+ pElement.getPaletteCategory());
		querStr.append("po:paletteConstructIsGroupedInPaletteCategory <" + pElement.getPaletteCategory() +"> ;");
		//System.out.println("    Element UsesImage property: "+ pElement.getUsesImage());
		//querStr.append("po:paletteElementUsesImage \"" + pElement.getUsesImage() +"\" ;"); //currently not used
		System.out.println("    Element Palette Image : "+ pElement.getThumbnailURL());
		querStr.append("po:paletteConstructHasPaletteThumbnail \"" + pElement.getThumbnailURL() +"\" ;");
		System.out.println("    Element Canvas Image: "+ pElement.getImageURL());
		querStr.append("po:paletteConstructHasModelImage \"" + pElement.getImageURL() +"\" ;");
		System.out.println("    Element Image width: "+ pElement.getWidth());
		querStr.append("po:paletteConstructHasWidth " + pElement.getWidth() +" ;");
		System.out.println("    Element Image height: "+ pElement.getHeight());
		querStr.append("po:paletteConstructHasHeight " + pElement.getHeight() +" ;");
		System.out.println("    Element representedLanguage: "+ pElement.getRepresentedLanguageClass());
		querStr.append("po:paletteConstructIsRelatedToModelingLanguageConstruct " + pElement.getRepresentedLanguageClass() +" ;");
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
		querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " rdf:type rdfs:Class . ");
		querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " rdfs:subClassOf <"+ pElement.getParentLanguageClass() + "> . ");
		querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " rdfs:label \"" + pElement.getLabel() + "\" . ");
		if(pElement.getComment()!=null && !"".equals(pElement.getComment()))
			querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " rdfs:comment \"" + pElement.getComment() + "\" . ");
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
		System.out.println("/Element received: " +json);

		Gson gson = new Gson();
		PaletteElement element = gson.fromJson(json, PaletteElement.class);

		ParameterizedSparqlString querStr = null;
		querStr = new ParameterizedSparqlString();
		querStr.append("INSERT DATA {");	
		if(element.getLanguageSubclasses()!=null && element.getLanguageSubclasses().size()!=0) {
			for(Answer languageSubclass: element.getLanguageSubclasses()) {			
				System.out.println("The selected language class is : "+languageSubclass.getLabel());

				//querStr.append("<" + languageSubclass + "> rdf:type rdfs:Class . ");
				String uuid = languageSubclass.getId().split("#")[1];
				//String uuid = element.getUuid();
				System.out.println("uuid: " + uuid);
				querStr.append("<" + languageSubclass.getId() + "> rdfs:subClassOf <"+ element.getRepresentedLanguageClass() + "> . ");		
				/** The assumption is that the palette element should be already available before creating a language subclass. 
				 * The below clause creates a parent-child relationship between the existing element and the element to be integrated **/
				querStr.append("<http://fhnw.ch/modelingEnvironment/PaletteOntology#" + uuid + "> po:paletteConstructHasParentPaletteConstruct <http://fhnw.ch/modelingEnvironment/PaletteOntology#"+ element.getParentElement() + "> . <http://fhnw.ch/modelingEnvironment/PaletteOntology#" + uuid + "> po:paletteConstructIsGroupedInPaletteCategory <" + element.getPaletteCategory() + "> . ");								
			}
		}
		querStr.append("}");
		//querStr.append(" WHERE { }");
		ontology.insertQuery(querStr);

		return Response.status(Status.OK).entity("{}").build();
	}

	@POST
	@Path("/deletePaletteElement")
	public Response deletePaletteElement(String json) {

		System.out.println("/Element received: " +json);

		Gson gson = new Gson();
		PaletteElement element = gson.fromJson(json, PaletteElement.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();

		/**
		 * Delete a class and all its predicates and values
		 * DELETE
		 * WHERE {bpmn:NewSubprocess ?predicate  ?object .}
		 */

		querStr.append("DELETE ");
		querStr.append("WHERE { <"+ element.getRepresentedLanguageClass() +"> ?predicate ?object . } ");
		//querStr.append("INSERT {"+"<"+element.getId()+"> rdfs:label "+element.getLabel());

		System.out.println(querStr.toString());
		//Model modelTpl = ModelFactory.createDefaultModel();
		ontology.insertQuery(querStr);

		querStr1.append("DELETE ");
		querStr1.append("WHERE { <"+ element.getId() +"> ?predicate ?object . } ");

		System.out.println(querStr1.toString());
		ontology.insertQuery(querStr1);

		return Response.status(Status.OK).entity("{}").build();

	}

	@POST
	@Path("/createCanvasInstance")//Not yet being used in webapp
	public Response insertCanvasInstance(String json) { 

		System.out.println("/element received: " +json);

		Gson gson = new Gson();
		GraphicalElement gElement = gson.fromJson(json, GraphicalElement.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		System.out.println("test: "+gElement.getUuid());
		querStr.append("INSERT {");
		System.out.println("    Element ID: " + gElement.getUuid());
		querStr.append("<"+gElement.getUuid()+">"  +" rdf:type " + "<"+gElement.getClassType()+">" + " ;");
		System.out.println("    Element Type: " + gElement.getClassType());
		querStr.append("po:graphicalElementClassType \"" + "<"+gElement.getClassType()+">" +"\" ;");
		System.out.println("    Element Label: "+ gElement.getLabel());
		querStr.append("rdfs:label \"" + gElement.getLabel() +"\" ;");
		System.out.println("    Element X Position: "+ gElement.getX());
		querStr.append("po:graphicalElementX \"" + gElement.getX() +"\" ;");
		System.out.println("    Element Y Position: "+ gElement.getY());
		querStr.append("po:graphicalElementY \"" + gElement.getY() +"\" ;");

		querStr.append("}");
		//Model modelTpl = ModelFactory.createDefaultModel();
		ontology.insertQuery(querStr);

		return Response.status(Status.OK).entity("{}").build();

	}

	@POST
	@Path("/modifyElement")
	public Response modifyElementLabel(@FormParam("element") String json, @FormParam("modifiedElement") String modifiedJson) {

		System.out.println("/Element received: " +json);
		System.out.println("/Modifed element: " + modifiedJson);

		Gson gson = new Gson();
		PaletteElement element = gson.fromJson(json, PaletteElement.class);
		PaletteElement modifiedElement = gson.fromJson(modifiedJson, PaletteElement.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();

		querStr.append("DELETE DATA { ");
		querStr.append("<"+element.getId()+"> rdfs:label \"" +element.getLabel()+ "\" . ");
		querStr.append("<"+element.getId()+"> po:paletteConstructHasModelImage \"" +element.getImageURL()+ "\" . ");
		querStr.append("<"+element.getId()+"> po:paletteConstructHasPaletteThumbnail \"" +element.getThumbnailURL()+ "\" . ");

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
		querStr1.append("<"+element.getId()+"> rdfs:label \""+modifiedElement.getLabel()+ "\" . ");
		querStr1.append("<"+element.getId()+"> po:paletteConstructHasModelImage \""+modifiedElement.getImageURL()+ "\" . ");
		querStr1.append("<"+element.getId()+"> po:paletteConstructHasPaletteThumbnail \"" +modifiedElement.getThumbnailURL()+ "\" . ");
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
		querStr.append("<"+element.getRepresentedLanguageClass()+"> rdfs:label \"" +element.getLabel()+ "\" . ");
		querStr.append("<"+element.getRepresentedLanguageClass()+"> rdfs:comment \"" +element.getComment()+ "\" . ");
		querStr.append(" }");
		querStr1.append("INSERT DATA { ");
		querStr1.append("<"+element.getRepresentedLanguageClass()+"> rdfs:label \""+modifiedElement.getLabel()+ "\" . ");
		querStr1.append("<"+element.getRepresentedLanguageClass()+"> rdfs:comment \""+modifiedElement.getComment()+ "\" . ");
		querStr1.append(" }");

		//Model modelTpl = ModelFactory.createDefaultModel();
		ontology.insertQuery(querStr);
		ontology.insertQuery(querStr1);

		return Response.status(Status.OK).entity("{}").build();

	}

	@POST
	@Path("/editDatatypeProperty")
	public Response editDatatypeProperty(@FormParam("property") String json, @FormParam("editedProperty") String modifiedJson) {

		System.out.println("/datatypeProperty received: " +json);
		System.out.println("/Modifed datatypeProperty: " + modifiedJson);

		Gson gson = new Gson();
		DatatypeProperty datatypeProperty = gson.fromJson(json, DatatypeProperty.class);
		DatatypeProperty modifiedDatatypeProperty = gson.fromJson(modifiedJson, DatatypeProperty.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();

		querStr.append("DELETE DATA { ");
		//querStr.append(datatypeProperty.getId() + " rdf:type owl:DataTypeProperty .");
		querStr.append("<"+datatypeProperty.getId() + "> rdfs:label \"" + datatypeProperty.getLabel() + "\" . ");
		querStr.append("<"+datatypeProperty.getId() + "> rdfs:range \"" + datatypeProperty.getRange() + "\" . ");
		querStr.append(" }");
		querStr1.append("INSERT DATA { ");
		//querStr1.append(datatypeProperty.getId() + " rdf:type owl:DataTypeProperty .");
		querStr1.append("<"+datatypeProperty.getId() + "> rdfs:label \"" + modifiedDatatypeProperty.getLabel() + "\" . ");
		querStr1.append("<"+datatypeProperty.getId() + "> rdfs:range " + modifiedDatatypeProperty.getRange() + " . ");
		querStr1.append(" }");

		//Model modelTpl = ModelFactory.createDefaultModel();
		ontology.insertQuery(querStr);
		ontology.insertQuery(querStr1);

		return Response.status(Status.OK).entity("{}").build();

	}
	
	@POST
	@Path("/editObjectProperty")
	public Response editObjectProperty(@FormParam("property") String json, @FormParam("editedProperty") String modifiedJson) {

		System.out.println("/objectProperty received: " +json);
		System.out.println("/Modifed objectProperty: " + modifiedJson);

		Gson gson = new Gson();
		ObjectProperty objectProperty = gson.fromJson(json, ObjectProperty.class);
		ObjectProperty modifiedObjectProperty = gson.fromJson(modifiedJson, ObjectProperty.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();

		querStr.append("DELETE DATA { ");
		//querStr.append(datatypeProperty.getId() + " rdf:type owl:DataTypeProperty .");
		querStr.append("<"+objectProperty.getId() + "> rdfs:label \"" + objectProperty.getLabel() + "\" . ");
		querStr.append("<"+objectProperty.getId() + "> rdfs:range <" + objectProperty.getRange() + "> . ");
		querStr.append(" }");
		querStr1.append("INSERT DATA { ");
		//querStr1.append(datatypeProperty.getId() + " rdf:type owl:DataTypeProperty .");
		querStr1.append("<"+objectProperty.getId() + "> rdfs:label \"" + modifiedObjectProperty.getLabel() + "\" . ");
		querStr1.append("<"+objectProperty.getId() + "> rdfs:range <" + modifiedObjectProperty.getRange() + "> . ");
		querStr1.append(" }");

		//Model modelTpl = ModelFactory.createDefaultModel();
		ontology.insertQuery(querStr);
		ontology.insertQuery(querStr1);

		return Response.status(Status.OK).entity("{}").build();

	}

	@POST
	@Path("/deleteDatatypeProperty")
	public Response deleteDatatypeProperty(String json) {

		System.out.println("/Element received: " +json);

		Gson gson = new Gson();
		DatatypeProperty property = gson.fromJson(json, DatatypeProperty.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();

		/**
		 * Delete a class and all its predicates and values
		 * DELETE
		 * WHERE {bpmn:NewSubprocess ?predicate  ?object .}
		 */

		querStr.append("DELETE "); //Does not work with DELETE DATA
		querStr.append("WHERE { <"+ property.getId() +"> ?predicate ?object . } ");

		ontology.insertQuery(querStr);
		return Response.status(Status.OK).entity("{}").build();
	}
	
	@POST
	@Path("/deleteObjectProperty")
	public Response deleteObjectProperty(String json) {

		System.out.println("/Element received: " +json);

		Gson gson = new Gson();
		ObjectProperty property = gson.fromJson(json, ObjectProperty.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();

		/**
		 * Delete a class and all its predicates and values
		 * DELETE
		 * WHERE {bpmn:NewSubprocess ?predicate  ?object .}
		 */

		querStr.append("DELETE ");
		querStr.append("WHERE { <"+ property.getId() +"> ?predicate ?object . } ");

		ontology.insertQuery(querStr);
		return Response.status(Status.OK).entity("{}").build();
	}


	@POST
	@Path("/createDomainElement")
	public Response insertDomainElement(String json) {

		System.out.println("/element received: " +json);

		Gson gson = new Gson();
		DomainElement pElement = gson.fromJson(json, DomainElement.class);
		//pElement.setClassType("http://fhnw.ch/modelingEnvironment/LanguageOntology#PaletteElement");

		ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();
		querStr1.append("INSERT DATA {");
		querStr1.append("do:" + pElement.getId() + " rdf:type rdfs:Class . ");
		if(pElement.isRoot() == false)
			querStr1.append("do:" + pElement.getId() + " rdfs:subClassOf <"+ pElement.getParentElement() + "> . ");
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

		System.out.println("/element received: " +json);

		Gson gson = new Gson();
		DatatypeProperty datatypeProperty = gson.fromJson(json, DatatypeProperty.class);
		//pElement.setClassType("http://fhnw.ch/modelingEnvironment/LanguageOntology#PaletteElement");

		if(datatypeProperty.getDomainName()!=null) {
			String domainName = datatypeProperty.getDomainName();

			if(!domainName.contains("#")) {
				String[] domainArr = domainName.split(":");
				domainName = GlobalVariables.getNamespaceMap().get(domainArr[0]) + "#" + domainArr[1];
				System.out.println("Domain range to insert :" +domainName);
			}


			ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();
			querStr1.append("INSERT DATA {");
			System.out.println("    Property ID: " + datatypeProperty.getId());
			querStr1.append("lo:" + datatypeProperty.getId() + " rdf:type owl:DatatypeProperty . ");
			System.out.println("    Language Class: " + datatypeProperty.getDomainName());
			querStr1.append("lo:" + datatypeProperty.getId() + " rdfs:domain "+ "<" + domainName + "> . ");
			System.out.println("    Property Label: " + datatypeProperty.getLabel());
			querStr1.append("lo:" + datatypeProperty.getId() + " rdfs:label \"" + datatypeProperty.getLabel() + "\" . ");
			System.out.println("    Property Range: " + datatypeProperty.getRange());
			querStr1.append("lo:" + datatypeProperty.getId() + " rdfs:range \"" + datatypeProperty.getRange() + "\" ");
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

		System.out.println("/element received: " +json);

		Gson gson = new Gson();
		ObjectProperty objectProperty = gson.fromJson(json, ObjectProperty.class);
		//pElement.setClassType("http://fhnw.ch/modelingEnvironment/LanguageOntology#PaletteElement");

		if(objectProperty.getDomainName()!=null) {
			String domainName = objectProperty.getDomainName();

			if(!domainName.contains("#")) {
				String[] domainArr = domainName.split(":");
				domainName = GlobalVariables.getNamespaceMap().get(domainArr[0]) + "#" + domainArr[1];
				System.out.println("Domain range to insert :" +domainName);
			}


			ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();
			querStr1.append("INSERT DATA {");
			System.out.println("    Property ID: " + objectProperty.getId());
			querStr1.append("lo:" + objectProperty.getId() + " rdf:type owl:ObjectProperty .");
			querStr1.append("lo:" + objectProperty.getId() + " rdfs:subPropertyOf lo:elementHasBridgingConcept . ");
			System.out.println("    Language Class: " + objectProperty.getDomainName());
			querStr1.append("lo:" + objectProperty.getId() + " rdfs:domain "+ "<" + domainName + "> . ");
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

		System.out.println("/element received: " +json);

		Gson gson = new Gson();
		ObjectProperty objectProperty = gson.fromJson(json, ObjectProperty.class);
		//pElement.setClassType("http://fhnw.ch/modelingEnvironment/LanguageOntology#PaletteElement");

		if(objectProperty.getDomainName()!=null) {
			String domainName = objectProperty.getDomainName();

			if(!domainName.contains("#")) {
				String[] domainArr = domainName.split(":");
				domainName = GlobalVariables.getNamespaceMap().get(domainArr[0]) + "#" + domainArr[1];
				System.out.println("Domain range to insert :" +domainName);
			}


			ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();
			querStr1.append("INSERT DATA {");
			System.out.println("    Property ID: " + objectProperty.getId());
			querStr1.append("lo:" + objectProperty.getId() + " rdf:type owl:ObjectProperty .");
			querStr1.append("lo:" + objectProperty.getId() + " rdfs:subPropertyOf lo:elementIsMappedWithDOConcept . ");
			System.out.println("    Language Class: " + objectProperty.getDomainName());
			querStr1.append("lo:" + objectProperty.getId() + " rdfs:domain "+ "<" + domainName + "> . ");
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

	@GET
	@Path("/getDomainOntologyClasses")
	public Response getDomainOntologyElements() {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested domain ontology elements" );
		System.out.println("####################<end>####################");
		ArrayList<Answer> all_do_elements = new ArrayList<Answer>();

		try {
			all_do_elements = queryDOElements();

			if (debug_properties){
				for (int index = 0; index < all_do_elements.size(); index++){
					System.out.println("Element "+index+": "+all_do_elements.get(index).getId());
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(all_do_elements);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " +json);
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
		System.out.println("/requested modeling language ontology elements" );
		System.out.println("####################<end>####################");
		ArrayList<Answer> all_ml_elements = new ArrayList<Answer>();

		try {
			all_ml_elements = queryMLElements();

			if (debug_properties){
				for (int index = 0; index < all_ml_elements.size(); index++){
					System.out.println("Element "+index+": "+all_ml_elements.get(index).getId());
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(all_ml_elements);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " +json);
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
		System.out.println("/requested datatype properties for " +domainName);
		System.out.println("####################<end>####################");
		ArrayList<DatatypeProperty> datatype_properties = new ArrayList<DatatypeProperty>();

		try {
			if(domainName != null) {
				String[] domainNameArr = domainName.split(":");
				domainName = GlobalVariables.getNamespaceMap().get(domainNameArr[0].toLowerCase()) + "#" + domainNameArr[1];
				System.out.println("domain range for query is : " +domainName);
				datatype_properties = queryAllDatatypeProperties(domainName);

				if (debug_properties){
					for (int index = 0; index < datatype_properties.size(); index++){
						System.out.println("Domain "+index+": ");
					}
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(datatype_properties);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " +json);
		System.out.println("####################<end>####################");
		return Response.status(Status.OK).entity(json).build();
	}

	private ArrayList<DatatypeProperty> queryAllDatatypeProperties(String domainName) throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<DatatypeProperty> result = new ArrayList<DatatypeProperty>();

		queryStr.append("SELECT DISTINCT ?id ?domain ?range ?label WHERE {");
		queryStr.append("?id a ?type . FILTER(?type IN (owl:DatatypeProperty)) . ");
		queryStr.append("?id rdfs:domain ?domain . ");
		queryStr.append("FILTER(?domain IN (<" + domainName + ">)) . ");
		queryStr.append("?id rdfs:label ?label . ");
		queryStr.append("?id rdfs:range ?range . ");
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
				datatypeProperty.setRange(soln.get("?range").toString());

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
		System.out.println("/requested datatype properties for " +domainName);
		System.out.println("####################<end>####################");
		ArrayList<ObjectProperty> object_properties = new ArrayList<ObjectProperty>();

		try {
			if(domainName != null) {
				String[] domainNameArr = domainName.split(":");
				domainName = GlobalVariables.getNamespaceMap().get(domainNameArr[0].toLowerCase()) + "#" + domainNameArr[1];
				System.out.println("domain range for query is : " +domainName);
				object_properties = queryAllBCObjectProperties(domainName);

				if (debug_properties){
					for (int index = 0; index < object_properties.size(); index++){
						System.out.println("Domain "+index+": ");
					}
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(object_properties);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " +json);
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
		System.out.println("/requested datatype properties for " +domainName);
		System.out.println("####################<end>####################");
		ArrayList<ObjectProperty> object_properties = new ArrayList<ObjectProperty>();

		try {
			if(domainName != null) {
				String[] domainNameArr = domainName.split(":");
				domainName = GlobalVariables.getNamespaceMap().get(domainNameArr[0].toLowerCase()) + "#" + domainNameArr[1];
				System.out.println("domain range for query is : " +domainName);
				object_properties = queryAllObjectProperties(domainName);

				if (debug_properties){
					for (int index = 0; index < object_properties.size(); index++){
						System.out.println("Domain "+index+": ");
					}
				}
			}
		} catch (NoResultsException e) {
			e.printStackTrace();
		}


		String json = gson.toJson(object_properties);
		System.out.println("\n####################<start>####################");
		System.out.println("/search genereated json: " +json);
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
	@Path("/getAllNamespacePrefixes")
	public Response getAllNamespacePrefixes() {
		ArrayList<String> prefixList = new ArrayList<String>();
		for (NAMESPACE ns : NAMESPACE.values()) {
			prefixList.add(ns.getPrefix()+":");
		}
		Collections.sort(prefixList);
		return Response.status(Status.OK).entity(gson.toJson(prefixList)).build();
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
}
