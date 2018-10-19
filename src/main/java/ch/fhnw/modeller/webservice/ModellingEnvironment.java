package ch.fhnw.modeller.webservice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.PathParam;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import com.google.gson.Gson;

import ch.fhnw.modeller.model.graphEnvironment.Answer;
import ch.fhnw.modeller.model.metamodel.DomainElement;
import ch.fhnw.modeller.model.metamodel.GraphicalElement;
import ch.fhnw.modeller.model.palette.PaletteCategory;
import ch.fhnw.modeller.model.palette.PaletteElement;
import ch.fhnw.modeller.model.metamodel.DatatypeProperty;
import ch.fhnw.modeller.webservice.exception.NoResultsException;
import ch.fhnw.modeller.webservice.ontology.FormatConverter;
import ch.fhnw.modeller.webservice.ontology.NAMESPACE;
import ch.fhnw.modeller.webservice.ontology.OntologyManager;
import ch.fhnw.modeller.persistence.GlobalVariables;

@Path("/ModEnv")
public class ModellingEnvironment {
	private Gson gson = new Gson();
	private OntologyManager ontology = OntologyManager.getInstance();
	private boolean debug_properties = false;

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
		
		queryStr.append("SELECT ?element ?label ?representedClass ?hidden ?category ?parent ?backgroundColor ?height ?iconPosition ?iconURL ?imageURL ?labelPosition ?shape ?thumbnailURL ?usesImage ?width ?borderColor ?borderType ?borderThickness ?type WHERE {");
		queryStr.append("?element rdf:type ?type . FILTER(?type IN (po:PaletteElement, po:PaletteConnector)) .");
		queryStr.append("?element rdfs:label ?label .");
		queryStr.append("?element po:paletteModelIsRelatedToModelingLanguageConstruct ?representedClass .");
		//queryStr.append("?element po:languageElementIsRelatedToDomainElement ?representedDomainClasses ."); //not sure how to read multiple values
		queryStr.append("?element po:hiddenFromPalette ?hidden .");
		queryStr.append("?element po:paletteModelHasPaletteCategory ?category .");
		queryStr.append("?element po:paletteElementUsesImage ?usesImage .");
		
		queryStr.append("OPTIONAL{ ?element po:paletteElementBackgroundColor ?backgroundColor }.");
		queryStr.append("OPTIONAL{ ?element po:paletteElementHeight ?height }.");
		queryStr.append("OPTIONAL{ ?element po:paletteElementIconPosition ?iconPosition }.");
		queryStr.append("OPTIONAL{ ?element po:paletteElementIconURL ?iconURL}.");
		queryStr.append("OPTIONAL{ ?element po:paletteElementImageURL ?imageURL }.");
		queryStr.append("OPTIONAL{ ?element po:paletteElementLabelPosition ?labelPosition }.");
		queryStr.append("OPTIONAL{ ?element po:paletteElementShape ?shape }.");
		queryStr.append("OPTIONAL{ ?element po:paletteElementThumbnailURL ?thumbnailURL }.");
		queryStr.append("OPTIONAL{ ?element po:paletteElementWidth ?width }.");
		queryStr.append("OPTIONAL{ ?element po:paletteElementBorderColor ?borderColor }.");
		queryStr.append("OPTIONAL{ ?element po:paletteElementBorderThickness ?borderThickness }.");
		queryStr.append("OPTIONAL{ ?element po:paletteElementBorderType ?borderType }.");
		queryStr.append("OPTIONAL{ ?element po:paletteModelHasParentPaletteModel ?parent }.");
		
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
				tempPaletteElement.setUsesImage(FormatConverter.ParseOntologyBoolean(soln.get("?usesImage").toString()));
				
				if (soln.get("?backgroundColor") != null){
					tempPaletteElement.setBackgroundColor(soln.get("?backgroundColor").toString());
				}
				
				if (soln.get("?height") != null){
					tempPaletteElement.setHeight(FormatConverter.ParseOntologyInteger(soln.get("?height").toString()));
				}
				if (soln.get("?iconPosition") != null){
					tempPaletteElement.setIconPosition(soln.get("?iconPosition").toString());
				}
				if (soln.get("?iconURL") != null){
					tempPaletteElement.setIconURL(soln.get("?iconURL").toString());
				}
				if (soln.get("?imageURL") != null){
					tempPaletteElement.setImageURL(soln.get("?imageURL").toString());
				}
				if (soln.get("?labelPosition") != null){
					tempPaletteElement.setLabelPosition(soln.get("?labelPosition").toString());
				}
				if (soln.get("?shape") != null){
					tempPaletteElement.setShape(soln.get("?shape").toString());
				}
				if (soln.get("?thumbnailURL") != null){
					tempPaletteElement.setThumbnailURL(soln.get("?thumbnailURL").toString());
				}
				if (soln.get("?width") != null){
					tempPaletteElement.setWidth(FormatConverter.ParseOntologyInteger(soln.get("?width").toString()));
				}
				if (soln.get("?borderColor") != null){
					tempPaletteElement.setBorderColor(soln.get("?borderColor").toString());
				}
				if (soln.get("?borderThickness") != null){
					tempPaletteElement.setBorderThickness(soln.get("?borderThickness").toString());
				}
				if (soln.get("?borderType") != null){
					tempPaletteElement.setBorderType(soln.get("?borderType").toString());
				}
				if (soln.get("?parent") != null){
					tempPaletteElement.setParentElement(soln.get("?parent").toString());
				}
				
				
				result.add(tempPaletteElement);
			}
		}
		qexec.close();
		return result;
}
	
	@GET
	@Path("/getPaletteCategories")
	public Response getPaletteCategories() {
		System.out.println("\n####################<start>####################");
		System.out.println("/requested palette categories" );
		System.out.println("####################<end>####################");
		ArrayList<PaletteCategory> all_palette_categories = new ArrayList<PaletteCategory>();
		
		try {
				all_palette_categories = queryAllPaletteCategories();

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
	
	private ArrayList<PaletteCategory> queryAllPaletteCategories() throws NoResultsException {
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
		ArrayList<PaletteCategory> result = new ArrayList<PaletteCategory>();
		
		queryStr.append("SELECT ?category ?label ?orderNumber ?hidden WHERE {");
		queryStr.append("?category rdf:type* po:PaletteCategory .");
		queryStr.append("?category rdfs:label ?label .");
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
				tempPaletteCategory.setId(soln.get("?category").toString());
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
	System.out.println("pre: " + all_palette_elements.size());
	for (int i = 0; i < all_palette_elements.size(); i++){
		if (all_palette_elements.get(i).getParentElement() == null){
			parentList.add(all_palette_elements.get(i));
		}
	}
	System.out.println("post: " + all_palette_elements.size());
	System.out.println("Number of parents: " + parentList.size());
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
			list.get(i).getParentElement().equals(parent.getId())){
				//System.out.println("2. Found a child of " + parent.getId() + " -> " + list.get(i).getId());
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
		System.out.println("    Element ID: " + pElement.getUuid());
			querStr.append("po:" + pElement.getUuid() + " po:hiddenFromPalette false;");
			querStr.append("}");
			
		ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();
		querStr1.append("INSERT DATA {");
		System.out.println("    Element ID: " + pElement.getUuid());
			querStr1.append("po:" + pElement.getUuid() + " po:hiddenFromPalette true;");
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
			querStr.append("po:hiddenFromPalette " + pElement.getHiddenFromPalette() +" ;");
		System.out.println("    Element Parent: "+ pElement.getParentElement());
			querStr.append("po:paletteModelHasParentPaletteModel <" + "http://fhnw.ch/modelingEnvironment/PaletteOntology#"+pElement.getParentElement() +"> ;");
		System.out.println("    Element Category: "+ pElement.getPaletteCategory());
			querStr.append("po:paletteModelHasPaletteCategory <" + pElement.getPaletteCategory() +"> ;");
		System.out.println("    Element UsesImage property: "+ pElement.getUsesImage());
			querStr.append("po:paletteElementUsesImage \"" + pElement.getUsesImage() +"\" ;");
		System.out.println("    Element Palette Image : "+ pElement.getThumbnailURL());
			querStr.append("po:paletteElementThumbnailURL \"" + pElement.getThumbnailURL() +"\" ;");
		System.out.println("    Element Canvas Image: "+ pElement.getImageURL());
			querStr.append("po:paletteElementImageURL \"" + pElement.getImageURL() +"\" ;");
		System.out.println("    Element Image width: "+ pElement.getWidth());
			querStr.append("po:paletteElementWidth " + pElement.getWidth() +" ;");
		System.out.println("    Element Image height: "+ pElement.getHeight());
			querStr.append("po:paletteElementHeight " + pElement.getHeight() +" ;");
		System.out.println("    Element representedLanguage: "+ pElement.getRepresentedLanguageClass());
			querStr.append("po:paletteModelIsRelatedToModelingLanguageConstruct " + pElement.getRepresentedLanguageClass() +" ;");
		if(pElement.getRepresentedDomainClass()!=null) {
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
		}
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
		querStr1.append("INSERT {");
		querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " rdf:type rdfs:Class . ");
		querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " rdfs:subClassOf <"+ pElement.getParentLanguageClass() + "> . ");
		querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " rdfs:label \"" + pElement.getUuid() + "\" . ");
		if(pElement.getRepresentedDomainClass()!=null && pElement.getRepresentedDomainClass().size()!=0) {
			for(String repDomainClass: pElement.getRepresentedDomainClass()) {
				System.out.println("The selected domain class is : "+repDomainClass);
				if(repDomainClass!=null && !"".equals(repDomainClass))
					querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " po:languageElementIsRelatedToDomainElement <" + repDomainClass + "> . ");
			}
		}
		querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " rdf:type <http://fhnw.ch/modelingEnvironment/PaletteOntology#PaletteElement> . ");
		querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " po:paletteModelHasParentPaletteModel <http://fhnw.ch/modelingEnvironment/PaletteOntology#" + pElement.getParentElement() +"> . ");
		querStr1.append(pElement.getLanguagePrefix() + pElement.getUuid() + " po:paletteModelIsRelatedToModelingLanguageConstruct " + pElement.getLanguagePrefix() + pElement.getUuid() + " . ");
		querStr1.append("}");
		querStr1.append(" WHERE { }");
		
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
		querStr.append("INSERT {");	
		if(element.getLanguageSubclasses()!=null && element.getLanguageSubclasses().size()!=0) {
			for(String languageSubclass: element.getLanguageSubclasses()) {			
				System.out.println("The selected language class is : "+languageSubclass);
					
				//querStr.append("<" + languageSubclass + "> rdf:type rdfs:Class . ");
				String uuid = languageSubclass.split("#")[1];
				System.out.println("uuid: " + uuid);
				querStr.append("<" + languageSubclass + "> rdfs:subClassOf <"+ element.getRepresentedLanguageClass() + "> . ");		
				querStr.append("<http://fhnw.ch/modelingEnvironment/PaletteOntology#" + uuid + "> po:paletteModelHasParentPaletteModel <http://fhnw.ch/modelingEnvironment/PaletteOntology#"+ element.getParentElement() + "> . ");								
			}
		}
		querStr.append("}");
		querStr.append(" WHERE { }");
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
		querStr.append("WHERE { <"+ element.getRepresentedLanguageClass() +"> ?object ?predicate . } ");
		//querStr.append("INSERT {"+"<"+element.getId()+"> rdfs:label "+element.getLabel());
		
		System.out.println(querStr.toString());
		//Model modelTpl = ModelFactory.createDefaultModel();
		ontology.insertQuery(querStr);
		
		querStr1.append("DELETE ");
		querStr1.append("WHERE { <"+ element.getId() +"> ?object ?predicate . } ");
		
		System.out.println(querStr1.toString());
		ontology.insertQuery(querStr1);
	
		return Response.status(Status.OK).entity("{}").build();

	}
	
	@POST
	@Path("/createCanvasInstance")
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
		querStr.append("<"+element.getId()+"> po:paletteElementImageURL \"" +element.getImageURL()+ "\" . ");
		querStr.append("<"+element.getId()+"> po:paletteElementThumbnailURL \"" +element.getThumbnailURL()+ "\" . ");
		querStr.append(" }");
		querStr1.append("INSERT DATA { ");
		querStr1.append("<"+element.getId()+"> rdfs:label \""+modifiedElement.getLabel()+ "\" . ");
		querStr1.append("<"+element.getId()+"> po:paletteElementImageURL \""+modifiedElement.getImageURL()+ "\" . ");
		querStr1.append("<"+element.getId()+"> po:paletteElementThumbnailURL \"" +modifiedElement.getThumbnailURL()+ "\" . ");
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
		querStr1.append("<"+datatypeProperty.getId() + "> rdfs:range \"" + modifiedDatatypeProperty.getRange() + "\" . ");
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
		
		querStr.append("DELETE ");
		querStr.append("WHERE { <"+ property.getId() +"> ?object ?predicate . } ");
		
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
		querStr1.append("INSERT {");
		querStr1.append("do:" + pElement.getId() + " rdf:type rdfs:Class . ");
		if(pElement.isRoot() == true)
			querStr1.append("do:" + pElement.getId() + " rdfs:subClassOf do:"+ pElement.getParentElement() + " . ");
		querStr1.append("do:" + pElement.getId() + " rdfs:label \"" + pElement.getLabel() + "\" ");
		querStr1.append("}");
		querStr1.append(" WHERE { }");
		
		System.out.println("Create subclass in Domain Ontology");
		System.out.println(querStr1.toString());
		ontology.insertQuery(querStr1);
		
	
		return Response.status(Status.OK).entity("{}").build();

	}
	
	@POST
	@Path("/createDatatypeProperty")
	public Response insertObjectProperty(String json) {
		
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
			querStr1.append("INSERT {");
			System.out.println("    Property ID: " + datatypeProperty.getId());
			querStr1.append("po:" + datatypeProperty.getId() + " rdf:type owl:DataTypeProperty . ");
			System.out.println("    Language Class: " + datatypeProperty.getDomainName());
			querStr1.append("po:" + datatypeProperty.getId() + " rdfs:domain "+ "<" + domainName + "> . ");
			System.out.println("    Property Label: " + datatypeProperty.getLabel());
			querStr1.append("po:" + datatypeProperty.getId() + " rdfs:label \"" + datatypeProperty.getLabel() + "\" . ");
			System.out.println("    Property Range: " + datatypeProperty.getRange());
			querStr1.append("po:" + datatypeProperty.getId() + " rdfs:range \"" + datatypeProperty.getRange() + "\" ");
			querStr1.append("}");
			querStr1.append(" WHERE { }");

			System.out.println("Create Datatype property");
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
		queryStr.append("?id a ?type . FILTER(?type IN (owl:DataTypeProperty)) . ");
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
}
