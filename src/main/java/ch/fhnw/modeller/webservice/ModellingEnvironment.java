package ch.fhnw.modeller.webservice;

import java.util.ArrayList;
import java.util.UUID;
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

import ch.fhnw.modeller.model.metamodel.MetamodelElement;
import ch.fhnw.modeller.model.palette.PaletteCategory;
import ch.fhnw.modeller.model.palette.PaletteElement;
import ch.fhnw.modeller.persistence.GlobalVariables;
import ch.fhnw.modeller.webservice.exception.NoResultsException;
import ch.fhnw.modeller.webservice.ontology.OntologyManager;
import ch.fhnw.modeller.webservice.ontology.FormatConverter;

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
		
		queryStr.append("SELECT ?element ?label ?representedClass ?hidden ?category ?parentElement ?backgroundColor ?height ?iconPosition ?iconURL ?imageURL ?labelPosition ?shape ?thumbnailURL ?usesImage ?width ?borderColor ?borderType ?borderThickness WHERE {");
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
		
		queryStr.append("}");
		//queryStr.append("ORDER BY ?domain ?field");

		QueryExecution qexec = ontology.query(queryStr);
		ResultSet results = qexec.execSelect();
		
		if (results.hasNext()) {
			while (results.hasNext()) {
				PaletteElement tempPaletteElement = new PaletteElement();
				
				QuerySolution soln = results.next();
				tempPaletteElement.setUuid(soln.get("?element").toString());
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
			list.get(i).getParentElement().equals(parent.getUuid())){
				//System.out.println("2. Found a child of " + parent.getId() + " -> " + list.get(i).getId());
				result.add(list.get(i));
			}
		}
		return result;
	}
	
	@POST
	@Path("/createElement")
	public Response getMsg(String json) {
		
		System.out.println("/element received: " +json);
		
		Gson gson = new Gson();
		MetamodelElement mElement = gson.fromJson(json, MetamodelElement.class);

		ParameterizedSparqlString querStr = new ParameterizedSparqlString();
		System.out.println("test: "+mElement.getUuid());
		querStr.append("INSERT {");
		querStr.append("<"+mElement.getUuid()+">"  +" rdf:type " + "<"+mElement.getClassType()+">" + " ;");
		System.out.println("    Element ID: " + mElement.getUuid());
		System.out.println("    Element Type: " + mElement.getClassType());
		querStr.append("rdfs:label \"" + mElement.getTextLabel() +"\" ;");
		System.out.println("    Element Label: "+ mElement.getTextLabel());
		System.out.println("The following properties need to be defined in the ontology and added here:");
		System.out.println("    [*]Element Top Position: "+ mElement.getTop());
		System.out.println("    [*]Element Left Position: "+ mElement.getLeft());

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
		
		querStr.append("DELETE {"+"<"+element.getUuid()+"> rdfs:label ?label}");
		querStr.append("INSERT {"+"<"+element.getUuid()+"> rdfs:label "+element.getLabel());
		
		//Model modelTpl = ModelFactory.createDefaultModel();
		ontology.insertQuery(querStr);
	
		return Response.status(Status.OK).entity("{}").build();

	}
	

}
