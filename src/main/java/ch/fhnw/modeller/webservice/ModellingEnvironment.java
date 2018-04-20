package ch.fhnw.modeller.webservice;

import java.util.ArrayList;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
import ch.fhnw.modeller.webservice.exception.NoResultsException;
import ch.fhnw.modeller.webservice.ontology.FormatConverter;
import ch.fhnw.modeller.webservice.ontology.OntologyManager;


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
		
		queryStr.append("SELECT ?element ?label ?representedClass ?hidden ?category ?parent ?backgroundColor ?height ?iconPosition ?iconURL ?imageURL ?labelPosition ?shape ?thumbnailURL ?usesImage ?width ?borderColor ?borderType ?borderThickness WHERE {");
		queryStr.append("?element rdf:type* lo:PaletteElement .");
		queryStr.append("?element rdfs:label ?label .");
		queryStr.append("?element lo:paletteModelIsRelatedToLanguageElement ?representedClass .");
		queryStr.append("?element lo:hiddenFromPalette ?hidden .");
		queryStr.append("?element lo:paletteModelHasPaletteCategory ?category .");
		queryStr.append("?element lo:paletteElementUsesImage ?usesImage .");
		
		queryStr.append("OPTIONAL{ ?element lo:paletteElementBackgroundColor ?backgroundColor }.");
		queryStr.append("OPTIONAL{ ?element lo:paletteElementHeight ?height }.");
		queryStr.append("OPTIONAL{ ?element lo:paletteElementIconPosition ?iconPosition }.");
		queryStr.append("OPTIONAL{ ?element lo:paletteElementIconURL ?iconURL}.");
		queryStr.append("OPTIONAL{ ?element lo:paletteElementImageURL ?imageURL }.");
		queryStr.append("OPTIONAL{ ?element lo:paletteElementLabelPosition ?labelPosition }.");
		queryStr.append("OPTIONAL{ ?element lo:paletteElementShape ?shape }.");
		queryStr.append("OPTIONAL{ ?element lo:paletteElementThumbnailURL ?thumbnailURL }.");
		queryStr.append("OPTIONAL{ ?element lo:paletteElementWidth ?width }.");
		queryStr.append("OPTIONAL{ ?element lo:paletteElementBorderColor ?borderColor }.");
		queryStr.append("OPTIONAL{ ?element lo:paletteElementBorderThickness ?borderThickness }.");
		queryStr.append("OPTIONAL{ ?element lo:paletteElementBorderType ?borderType }.");
		queryStr.append("OPTIONAL{ ?element lo:paletteModelHasParentPaletteModel ?parent }.");
		
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
		queryStr.append("?category rdf:type* lo:PaletteCategory .");
		queryStr.append("?category rdfs:label ?label .");
		queryStr.append("OPTIONAL {?category lo:paletteCategoryOrderNumber ?orderNumber . }");
		queryStr.append("OPTIONAL {?category lo:hiddenFromPalette ?hidden . }");
		
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
		querStr.append("lo:" + pElement.getUuid()  +" rdf:type " + "<http://fhnw.ch/modelingEnvironment/LanguageOntology#PaletteElement>" + " ;");
		/*System.out.println("    Element Type: " + pElement.getClassType());
			querStr.append("lo:graphicalElementClassType \"" + "<"+pElement.getClassType()+">" +"\" ;");*/
		System.out.println("    Element Label: "+ pElement.getLabel());
			querStr.append("rdfs:label \"" +pElement.getLabel() +"\" ;");
		System.out.println("    Element Hidden property: "+ pElement.getHiddenFromPalette());
			querStr.append("lo:hiddenFromPalette \"" + pElement.getHiddenFromPalette() +"\" ;");
		System.out.println("    Element Parent: "+ pElement.getParentElement());
			querStr.append("lo:paletteModelHasParentPaletteModel <" + "http://fhnw.ch/modelingEnvironment/LanguageOntology#"+pElement.getParentElement() +"> ;");
		System.out.println("    Element Category: "+ pElement.getPaletteCategory());
			querStr.append("lo:paletteModelHasPaletteCategory <" + pElement.getPaletteCategory() +"> ;");
		System.out.println("    Element UsesImage property: "+ pElement.getUsesImage());
			querStr.append("lo:paletteElementUsesImage \"" + pElement.getUsesImage() +"\" ;");
		System.out.println("    Element representedLanguage: "+ pElement.getRepresentedLanguageClass());
			querStr.append("lo:paletteModelIsRelatedToLanguageElement <" + pElement.getRepresentedLanguageClass() +"> ;");
		/*System.out.println("    Element X Position: "+ pElement.getX());
			querStr.append("lo:graphicalElementX \"" + pElement.getX() +"\" ;");
		System.out.println("    Element Y Position: "+ pElement.getY());
			querStr.append("lo:graphicalElementY \"" + pElement.getY() +"\" ;");*/
			

			querStr.append("}");
		//Model modelTpl = ModelFactory.createDefaultModel();
			
		//ontology.insertQuery(querStr);
		
		querStr.clearParams();
		ParameterizedSparqlString querStr1 = new ParameterizedSparqlString();
		querStr1.append("INSERT {");
		querStr1.append("bpmn:" + pElement.getUuid() + " rdf:type rdfs:Class . ");
		querStr1.append("bpmn:" + pElement.getUuid() + " rdfs:subClassOf <http://ikm-group.ch/archiMEO/BPMN#"+ pElement.getParentElement() + "> . ");
		querStr1.append("bpmn:" + pElement.getUuid() + " rdfs:label \"" + pElement.getUuid() + "\" . ");
		if(pElement.getRepresentedDomainClass()!=null && !"".equals(pElement.getRepresentedDomainClass()))
			querStr1.append("bpmn:" + pElement.getUuid() + " lo:languageElementIsRelatedToDomainElement \"" + pElement.getRepresentedDomainClass() + "\" ");
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
			querStr.append("lo:graphicalElementClassType \"" + "<"+gElement.getClassType()+">" +"\" ;");
		System.out.println("    Element Label: "+ gElement.getLabel());
			querStr.append("rdfs:label \"" + gElement.getLabel() +"\" ;");
		System.out.println("    Element X Position: "+ gElement.getX());
			querStr.append("lo:graphicalElementX \"" + gElement.getX() +"\" ;");
		System.out.println("    Element Y Position: "+ gElement.getY());
			querStr.append("lo:graphicalElementY \"" + gElement.getY() +"\" ;");

			querStr.append("}");
		//Model modelTpl = ModelFactory.createDefaultModel();
		ontology.insertQuery(querStr);
	
		return Response.status(Status.OK).entity("{}").build();

	}
	
	@POST
	@Path("/modifyElementLabel")
	public Response modifyElementLabel(String json) {
		
		System.out.println("/Element received: " +json);
		
		Gson gson = new Gson();
		PaletteElement element = gson.fromJson(json, PaletteElement.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		
		querStr.append("DELETE {"+"<"+element.getId()+"> rdfs:label ?label}");
		querStr.append("INSERT {"+"<"+element.getId()+"> rdfs:label "+element.getLabel());
		
		//Model modelTpl = ModelFactory.createDefaultModel();
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
		queryStr.append("FILTER(STRSTARTS(STR(?id),STR(\"http://fhnw.ch/modelingEnvironment/DomainOntology#\"))) .");
		queryStr.append("}");
		queryStr.append("ORDER BY ?label");

		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();
		
		if (results.hasNext()) {
			while (results.hasNext()) {
				Answer ans = new Answer();
				
				QuerySolution soln = results.next();
				ans.setId(soln.get("?id").toString());
				ans.setLabel(soln.get("?label").toString());
				
				result.add(ans);
			}
		}
		qexec.close();
		
		return result;
}
	

}
